package com.ecommerce.nivora.product.service;

import com.ecommerce.nivora.cart.repository.CartItemRepository;
import com.ecommerce.nivora.category.dto.response.CategoryResponse;
import com.ecommerce.nivora.category.entity.Category;
import com.ecommerce.nivora.category.entity.CategoryMain;
import com.ecommerce.nivora.category.repository.CategoryRepository;
import com.ecommerce.nivora.category.repository.CategoryMainRepository;
import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.common.utils.SlugUtils;
import com.ecommerce.nivora.exception.ConflictException;
import com.ecommerce.nivora.exception.ResourceNotFoundException;
import com.ecommerce.nivora.exception.dto.request.BadRequestException;
import com.ecommerce.nivora.product.dto.request.ProductRequest;
import com.ecommerce.nivora.product.dto.response.ProductImageResponse;
import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.product.dto.response.ProductVariantResponse;
import com.ecommerce.nivora.product.entity.Inventory;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.product.entity.ProductImage;
import com.ecommerce.nivora.product.entity.ProductVariant;
import com.ecommerce.nivora.product.repository.InventoryRepository;
import com.ecommerce.nivora.product.repository.ProductImageRepository;
import com.ecommerce.nivora.product.repository.ProductRepository;
import com.ecommerce.nivora.product.repository.ProductVariantRepository;
import com.ecommerce.nivora.review.repository.ReviewRepository;
import com.ecommerce.nivora.upload.service.UploadService;
import com.ecommerce.nivora.wishlist.repository.WishlistItemRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final jakarta.servlet.http.HttpServletRequest httpServletRequest;
    private final Map<String, Long> viewCooldownCache = Collections.synchronizedMap(
        new LinkedHashMap<String, Long>(1000, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > 10000;
            }
        }
    );
    private final CategoryRepository categoryRepository;
    private final CategoryMainRepository categoryMainRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final InventoryRepository inventoryRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ReviewRepository reviewRepository;
    private final UploadService uploadService;
    private final SlugUtils slugUtils;
    private final ProductIndexService productIndexService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAll(Long categoryId, String categorySlug, String categoryMainSlug, String brand, BigDecimal minPrice,
            BigDecimal maxPrice, BigDecimal minRating,
            String search, Pageable pageable) {
        List<Long> categoryIds = null;
        if (categorySlug != null && !categorySlug.trim().isEmpty() && !"All".equalsIgnoreCase(categorySlug)) {
            categoryIds = getCategoryAndDescendantIds(categorySlug);
        } else if (categoryMainSlug != null && !categoryMainSlug.trim().isEmpty()) {
            CategoryMain main = categoryMainRepository.findBySlugAndDeletedAtIsNull(categoryMainSlug)
                    .orElseThrow(() -> new ResourceNotFoundException("CategoryMain not found: " + categoryMainSlug));
            List<Category> cats = categoryRepository.findByDeletedAtIsNullOrderByDisplayOrderAsc().stream()
                    .filter(c -> c.getCategoryMain() != null && c.getCategoryMain().getId().equals(main.getId()))
                    .collect(Collectors.toList());
            categoryIds = new ArrayList<>();
            for (Category c : cats) {
                collectDescendantIds(c, categoryIds);
            }
        } else if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
            categoryIds = new ArrayList<>();
            collectDescendantIds(category, categoryIds);
        }

        boolean applyCategoryFilter = categoryIds != null && !categoryIds.isEmpty();
        String sanitizedSearch = sanitizeSearchQuery(search);

        Page<Product> page = productRepository.findWithFilters(
                applyCategoryFilter, categoryIds, brand, minPrice, maxPrice, minRating, sanitizedSearch, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    private List<Long> getCategoryAndDescendantIds(String categorySlug) {
        Category category = categoryRepository.findBySlugAndDeletedAtIsNull(categorySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categorySlug));
        List<Long> ids = new ArrayList<>();
        collectDescendantIds(category, ids);
        return ids;
    }

    private void collectDescendantIds(Category category, List<Long> ids) {
        ids.add(category.getId());
        if (category.getChildren() != null) {
            for (Category child : category.getChildren()) {
                if (child.getDeletedAt() == null && child.getIsActive()) {
                    collectDescendantIds(child, ids);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllForAdmin(Long categoryId, String search, Pageable pageable) {
        String sanitizedSearch = sanitizeSearchQuery(search);
        Page<Product> page = productRepository.findAllForAdmin(categoryId, sanitizedSearch, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    private String sanitizeSearchQuery(String search) {
        if (search == null || search.trim().isEmpty()) {
            return null;
        }
        String normalized = search.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
        }
        return normalized.replace("\\", "\\\\")
                         .replace("%", "\\%")
                         .replace("_", "\\_");
    }

    private void incrementProductView(Product product) {
        String sessionId = httpServletRequest.getHeader("X-Session-Id");
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = httpServletRequest.getRemoteAddr();
        }
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "unknown";
        }
        
        String cacheKey = sessionId + ":" + product.getId();
        long now = System.currentTimeMillis();
        
        synchronized (viewCooldownCache) {
            Long lastViewTime = viewCooldownCache.get(cacheKey);
            if (lastViewTime != null && (now - lastViewTime) <= 60000) {
                return;
            }
            viewCooldownCache.put(cacheKey, now);
        }
        
        product.setViews(product.getViews() == null ? 1 : product.getViews() + 1);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public ProductResponse getById(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        incrementProductView(product);
        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getByIdForAdmin(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse getBySlug(String slug) {
        Product product = productRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
        incrementProductView(product);
        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeatured() {
        return productRepository.findByIsFeaturedTrueAndIsActiveTrueAndDeletedAtIsNull(PageRequest.of(0, 8))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getBestSellers() {
        return productRepository.findByIsBestSellerTrueAndIsActiveTrueAndDeletedAtIsNull(PageRequest.of(0, 8))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getNewArrivals() {
        return productRepository.findByIsNewTrueAndIsActiveTrueAndDeletedAtIsNull(PageRequest.of(0, 8))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getTrendingFallback(int limit) {
        return productRepository.findTrendingFallback(PageRequest.of(0, limit))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getRelated(Long productId, int limit) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        return productRepository.findRelated(product.getCategory().getId(), productId, PageRequest.of(0, limit))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return productRepository.findAllById(ids).stream()
                .filter(p -> p.getDeletedAt() == null && Boolean.TRUE.equals(p.getIsActive()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, ProductResponse> getActiveProductsBySkus(Collection<String> skus) {
        if (skus == null || skus.isEmpty()) {
            return Map.of();
        }

        List<String> skuList = skus.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (skuList.isEmpty()) {
            return Map.of();
        }

        List<Product> products = productRepository.findBySkuInAndDeletedAtIsNullAndIsActiveTrue(skuList);
        Map<String, ProductResponse> result = new HashMap<>();

        for (Product product : products) {
            ProductResponse response = toResponse(product);
            for (ProductVariant variant : product.getVariants()) {
                if (variant.getDeletedAt() == null
                        && Boolean.TRUE.equals(variant.getIsActive())
                        && skuList.contains(variant.getSku())) {
                    result.putIfAbsent(variant.getSku(), response);
                }
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, ProductResponse> getActiveProductsByUuids(Collection<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return Map.of();
        }

        List<String> uuidList = uuids.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (uuidList.isEmpty()) {
            return Map.of();
        }

        List<Product> products = productRepository.findByUuidInAndIsActiveTrueAndDeletedAtIsNull(uuidList);
        Map<String, ProductResponse> result = new HashMap<>();
        for (Product product : products) {
            if (product.getUuid() != null) {
                result.put(product.getUuid(), toResponse(product));
            }
        }
        return result;
    }


    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        if (categoryRepository.existsByParentIdAndDeletedAtIsNull(category.getId())) {
            throw new BadRequestException("Sản phẩm chỉ có thể thuộc về danh mục lá (leaf category)");
        }

        if (request.getVariants() == null || request.getVariants().isEmpty()) {
            throw new BadRequestException("Sản phẩm phải có ít nhất một phiên bản (variant)");
        }

        String slug = slugUtils.uniqueSlug(request.getName(), productRepository::existsBySlug);

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .slug(slug)
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .brand(request.getBrand())
                .productUrl(request.getProductUrl())
                .taxRate(request.getTaxRate() != null ? request.getTaxRate() : BigDecimal.ZERO)
                .weight(request.getWeight())
                .isActive(request.getIsActive())
                .isFeatured(request.getIsFeatured())
                .isNew(request.getIsNew())
                .isBestSeller(request.getIsBestSeller())
                .seoTitle(request.getSeoTitle())
                .seoDescription(request.getSeoDescription())
                .imageUrl(request.getImageUrl())
                .attributes(request.getAttributes() != null ? request.getAttributes() : "{}")
                .build();

        // Images
        List<ProductImage> images = new ArrayList<>();
        if (request.getImageUrls() != null) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                images.add(ProductImage.builder()
                        .product(product)
                        .url(request.getImageUrls().get(i))
                        .sortOrder(i)
                        .isPrimary(i == 0)
                        .build());
            }
        }

        // Variants
        List<ProductVariant> variants = new ArrayList<>();
        int sort = 0;
        for (ProductRequest.VariantRequest vr : request.getVariants()) {
            if (variantRepository.existsBySku(vr.getSku())) {
                throw new ConflictException("Variant SKU already exists: " + vr.getSku());
            }
            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .sku(vr.getSku())
                    .name(vr.getName())
                    .price(vr.getPrice())
                    .comparePrice(vr.getComparePrice())
                    .color(vr.getColor())
                    .size(vr.getSize())
                    .attributes(vr.getAttributes() != null ? vr.getAttributes() : "{}")
                    .imageUrl(vr.getImageUrl())
                    .isActive(vr.getIsActive())
                    .sortOrder(sort++)
                    .build();

            Inventory inventory = Inventory.builder()
                    .variant(variant)
                    .quantity(vr.getStock() != null ? vr.getStock() : 0)
                    .build();
            variant.setInventory(inventory);
            variants.add(variant);

            // Optional: Support variant_id in product_images (link to existing image if duplicate)
            if (vr.getImageUrl() != null && !vr.getImageUrl().isBlank()) {
                ProductImage existingImg = images.stream()
                        .filter(img -> img.getUrl().equals(vr.getImageUrl()))
                        .findFirst()
                        .orElse(null);
                if (existingImg != null) {
                    existingImg.setVariant(variant);
                } else {
                    images.add(ProductImage.builder()
                            .product(product)
                            .variant(variant)
                            .url(vr.getImageUrl())
                            .sortOrder(sort)
                            .isPrimary(false)
                            .build());
                }
            }
        }
        product.setImages(images);
        product.setVariants(variants);

        productRepository.save(product);
        productIndexService.indexProductAsync(product.getUuid());
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        if (categoryRepository.existsByParentIdAndDeletedAtIsNull(category.getId())) {
            throw new BadRequestException("Sản phẩm chỉ có thể thuộc về danh mục lá (leaf category)");
        }

        product.setCategory(category);
        product.setName(request.getName());
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setProductUrl(request.getProductUrl());
        product.setTaxRate(request.getTaxRate() != null ? request.getTaxRate() : BigDecimal.ZERO);
        product.setWeight(request.getWeight());
        product.setSeoTitle(request.getSeoTitle());
        product.setSeoDescription(request.getSeoDescription());
        product.setIsActive(request.getIsActive());
        product.setIsFeatured(request.getIsFeatured());
        product.setIsNew(request.getIsNew());
        product.setIsBestSeller(request.getIsBestSeller());
        product.setImageUrl(request.getImageUrl());
        if (request.getAttributes() != null) {
            product.setAttributes(request.getAttributes());
        }

        if (request.getImageUrls() != null) {
            product.getImages().clear();
            List<ProductImage> images = new ArrayList<>();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                images.add(ProductImage.builder()
                        .product(product)
                        .url(request.getImageUrls().get(i))
                        .sortOrder(i)
                        .isPrimary(i == 0)
                        .build());
            }
            product.getImages().addAll(images);
        }

        // Update Variants
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            java.util.Map<String, ProductVariant> existingVariants = product.getVariants().stream()
                    .collect(Collectors.toMap(ProductVariant::getSku, v -> v));

            List<ProductVariant> updatedVariants = new ArrayList<>();
            int sort = 0;
            for (ProductRequest.VariantRequest vr : request.getVariants()) {
                ProductVariant variant = existingVariants.get(vr.getSku());
                if (variant != null) {
                    // Update existing variant
                    variant.setName(vr.getName());
                    variant.setPrice(vr.getPrice());
                    variant.setComparePrice(vr.getComparePrice());
                    variant.setColor(vr.getColor());
                    variant.setSize(vr.getSize());
                    variant.setIsActive(vr.getIsActive());
                    variant.setSortOrder(sort++);
                    if (vr.getAttributes() != null) {
                        variant.setAttributes(vr.getAttributes());
                    }
                    variant.setImageUrl(vr.getImageUrl());
                    
                    // Update inventory
                    if (variant.getInventory() != null) {
                        variant.getInventory().setQuantity(vr.getStock() != null ? vr.getStock() : 0);
                    } else {
                        Inventory inventory = Inventory.builder()
                                .variant(variant)
                                .quantity(vr.getStock() != null ? vr.getStock() : 0)
                                .build();
                        variant.setInventory(inventory);
                    }
                    updatedVariants.add(variant);
                    existingVariants.remove(vr.getSku());
                } else {
                    // Create new variant
                    if (variantRepository.existsBySku(vr.getSku())) {
                        throw new ConflictException("Variant SKU already exists: " + vr.getSku());
                    }
                    ProductVariant newVariant = ProductVariant.builder()
                            .product(product)
                            .sku(vr.getSku())
                            .name(vr.getName())
                            .price(vr.getPrice())
                            .comparePrice(vr.getComparePrice())
                            .color(vr.getColor())
                            .size(vr.getSize())
                            .attributes(vr.getAttributes() != null ? vr.getAttributes() : "{}")
                            .imageUrl(vr.getImageUrl())
                            .isActive(vr.getIsActive())
                            .sortOrder(sort++)
                            .build();

                    Inventory inventory = Inventory.builder()
                            .variant(newVariant)
                            .quantity(vr.getStock() != null ? vr.getStock() : 0)
                            .build();
                    newVariant.setInventory(inventory);
                    updatedVariants.add(newVariant);
                }
            }

            // Remove/Delete variants that are no longer in the request
            for (ProductVariant oldVariant : existingVariants.values()) {
                if (oldVariant.getInventory() != null) {
                    inventoryRepository.delete(oldVariant.getInventory());
                }
                variantRepository.delete(oldVariant);
            }

            product.getVariants().clear();
            product.getVariants().addAll(updatedVariants);

            // Re-sync variant images to product.getImages() (link to existing image if duplicate)
            for (ProductVariant v : updatedVariants) {
                if (v.getImageUrl() != null && !v.getImageUrl().isBlank()) {
                    ProductImage existingImg = product.getImages().stream()
                            .filter(img -> img.getUrl().equals(v.getImageUrl()))
                            .findFirst()
                            .orElse(null);
                    if (existingImg != null) {
                        existingImg.setVariant(v);
                    } else {
                        product.getImages().add(ProductImage.builder()
                                .product(product)
                                .variant(v)
                                .url(v.getImageUrl())
                                .sortOrder(product.getImages().size())
                                .isPrimary(false)
                                .build());
                    }
                }
            }
        }

        productRepository.save(product);
        productIndexService.indexProductAsync(product.getUuid());
        return toResponse(product);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        productIndexService.deleteProductAsync(product.getUuid());

        // Fast path: mark deleted immediately so UI can update after response,
        // then clean up heavy relations in a background task.
        product.setDeletedAt(LocalDateTime.now());
        reviewRepository.deleteByProductId(id);
        wishlistItemRepository.deleteByProductId(id);
        cartItemRepository.deleteByProductId(id);
        imageRepository.deleteByProductId(id);

        var images = new ArrayList<>(product.getImages());
        var variants = new ArrayList<>(product.getVariants());
        product.getImages().clear();
        product.getVariants().clear();

        images.forEach(img -> {
            try {
                if (img.getUrl() != null && img.getUrl().contains("cloudinary.com")) {
                    String publicId = extractPublicIdFromUrl(img.getUrl());
                    if (publicId != null)
                        uploadService.deleteImage(publicId);
                }
            } catch (Exception ignored) {
            }
        });

        variants.forEach(v -> {
            try {
                if (v.getInventory() != null) {
                    inventoryRepository.delete(v.getInventory());
                }
                if (v.getImageUrl() != null && v.getImageUrl().contains("cloudinary.com")) {
                    String publicId = extractPublicIdFromUrl(v.getImageUrl());
                    if (publicId != null)
                        uploadService.deleteImage(publicId);
                }
            } catch (Exception ignored) {
            }
        });
        variantRepository.deleteAll(variants);
        productRepository.delete(product);
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            int idx = url.indexOf("/upload/");
            if (idx < 0)
                return null;
            String path = url.substring(idx + 8);
            int versionIdx = path.indexOf('/');
            if (versionIdx < 0)
                return null;
            String withoutVersion = path.substring(versionIdx + 1);
            int dot = withoutVersion.lastIndexOf('.');
            if (dot > 0)
                withoutVersion = withoutVersion.substring(0, dot);
            return withoutVersion;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariants(Long productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        return product.getVariants().stream()
                .filter(v -> v.getDeletedAt() == null)
                .map(this::toVariantResponse)
                .collect(Collectors.toList());
    }

    // ── Mapper ───────────────────────────────────────────

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .uuid(p.getUuid())
                .name(p.getName())
                .slug(p.getSlug())
                .size(p.getSize())
                .color(p.getColor())
                .shortDescription(p.getShortDescription())
                .description(p.getDescription())
                .brand(p.getBrand())
                .sku(p.getSku())
                .stock(p.getStock())
                .views(p.getViews())
                .basePrice(p.getBasePrice())
                .comparePrice(p.getComparePrice())
                .taxRate(p.getTaxRate())
                .isActive(p.getIsActive())
                .isFeatured(p.getIsFeatured())
                .isNew(p.getIsNew())
                .isBestSeller(p.getIsBestSeller())
                .ratingAvg(p.getRatingAvg())
                .ratingCount(p.getRatingCount())
                .imageUrl(p.getImageUrl())
                .category(p.getCategory() != null ? toCategoryResponse(p.getCategory()) : null)
                .images(p.getImages().stream().map(this::toImageResponse).collect(Collectors.toList()))
                .variants(p.getVariants().stream().map(this::toVariantResponse).collect(Collectors.toList()))
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private CategoryResponse toCategoryResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId()).name(c.getName()).slug(c.getSlug())
                .imageUrl(c.getImageUrl()).icon(c.getIcon())
                .build();
    }

    private ProductImageResponse toImageResponse(ProductImage img) {
        return ProductImageResponse.builder()
                .id(img.getId()).url(img.getUrl()).altText(img.getAltText())
                .sortOrder(img.getSortOrder()).isPrimary(img.getIsPrimary())
                .variantId(img.getVariant() != null ? img.getVariant().getId() : null)
                .build();
    }

    private ProductVariantResponse toVariantResponse(ProductVariant v) {
        int stock = 0, available = 0;
        if (v.getInventory() != null) {
            stock = v.getInventory().getQuantity();
            available = v.getInventory().getAvailableQuantity();
        }
        return ProductVariantResponse.builder()
                .id(v.getId()).sku(v.getSku()).name(v.getName())
                .price(v.getPrice()).comparePrice(v.getComparePrice())
                .color(v.getColor()).size(v.getSize())
                .attributes(v.getAttributes()).imageUrl(v.getImageUrl())
                .isActive(v.getIsActive()).sortOrder(v.getSortOrder())
                .stock(stock).availableStock(available)
                .build();
    }
}
