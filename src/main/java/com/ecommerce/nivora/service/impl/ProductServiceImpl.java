package com.ecommerce.nivora.service.impl;

import com.ecommerce.nivora.common.PageResponse;
import com.ecommerce.nivora.dto.request.ProductRequest;
import com.ecommerce.nivora.dto.response.*;
import com.ecommerce.nivora.entity.*;
import com.ecommerce.nivora.exception.ConflictException;
import com.ecommerce.nivora.exception.ResourceNotFoundException;
import com.ecommerce.nivora.repository.*;
import com.ecommerce.nivora.service.ProductService;
import com.ecommerce.nivora.service.UploadService;
import com.ecommerce.nivora.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final InventoryRepository inventoryRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ReviewRepository reviewRepository;
    private final UploadService uploadService;
    private final SlugUtils slugUtils;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAll(Long categoryId, String brand, BigDecimal minPrice,
            BigDecimal maxPrice, BigDecimal minRating,
            String search, Pageable pageable) {
        Page<Product> page = productRepository.findWithFilters(
                categoryId, brand, minPrice, maxPrice, minRating, search, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllForAdmin(Long categoryId, String search, Pageable pageable) {
        Page<Product> page = productRepository.findAllForAdmin(categoryId, search, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
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
    @Transactional(readOnly = true)
    public ProductResponse getBySlug(String slug) {
        Product product = productRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
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
    public List<ProductResponse> getRelated(Long productId, int limit) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        return productRepository.findRelated(product.getCategory().getId(), productId, PageRequest.of(0, limit))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (request.getSku() != null && productRepository.existsBySku(request.getSku())) {
            throw new ConflictException("SKU already exists: " + request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        String slug = slugUtils.uniqueSlug(request.getName(), productRepository::existsBySlug);

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .slug(slug)
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .brand(request.getBrand())
                .sku(request.getSku())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .basePrice(request.getBasePrice())
                .comparePrice(request.getComparePrice())
                .costPrice(request.getCostPrice())
                .taxRate(request.getTaxRate() != null ? request.getTaxRate() : BigDecimal.ZERO)
                .weight(request.getWeight())
                .isActive(request.getIsActive())
                .isFeatured(request.getIsFeatured())
                .isNew(request.getIsNew())
                .isBestSeller(request.getIsBestSeller())
                .seoTitle(request.getSeoTitle())
                .seoDescription(request.getSeoDescription())
                .build();

        // Images
        if (request.getImageUrls() != null) {
            List<ProductImage> images = new ArrayList<>();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                images.add(ProductImage.builder()
                        .product(product)
                        .url(request.getImageUrls().get(i))
                        .sortOrder(i)
                        .isPrimary(i == 0)
                        .build());
            }
            product.setImages(images);
        }

        // Variants
        List<ProductVariant> variants = new ArrayList<>();
        if (request.getSku() != null && !request.getSku().isBlank()) {
            boolean skuExists = request.getVariants() != null
                    && request.getVariants().stream().anyMatch(v -> request.getSku().equalsIgnoreCase(v.getSku()));
            if (!skuExists && variantRepository.existsBySku(request.getSku())) {
                throw new ConflictException("Variant SKU already exists: " + request.getSku());
            }
            ProductVariant defaultVariant = ProductVariant.builder()
                    .product(product)
                    .sku(request.getSku())
                    .name(request.getName())
                    .price(request.getBasePrice())
                    .comparePrice(request.getComparePrice())
                    .attributes("{}")
                    .imageUrl(request.getImageUrls() != null && !request.getImageUrls().isEmpty()
                            ? request.getImageUrls().get(0)
                            : null)
                    .isActive(request.getIsActive())
                    .sortOrder(0)
                    .build();
            Inventory inv = Inventory.builder()
                    .variant(defaultVariant)
                    .quantity(request.getStock() != null ? request.getStock() : 0)
                    .build();
            defaultVariant.setInventory(inv);
            variants.add(defaultVariant);
        }
        if (request.getVariants() != null) {
            int sort = variants.size();
            for (ProductRequest.VariantRequest vr : request.getVariants()) {
                if (request.getSku() != null && request.getSku().equalsIgnoreCase(vr.getSku()))
                    continue;
                if (variantRepository.existsBySku(vr.getSku())) {
                    throw new ConflictException("Variant SKU already exists: " + vr.getSku());
                }
                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .sku(vr.getSku())
                        .name(vr.getName())
                        .price(vr.getPrice())
                        .comparePrice(vr.getComparePrice())
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
            }
        }
        product.setVariants(variants);

        productRepository.save(product);
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        product.setCategory(category);
        product.setName(request.getName());
        product.setShortDescription(request.getShortDescription());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setSku(request.getSku());
        product.setStock(request.getStock() != null ? request.getStock() : product.getStock());
        product.setBasePrice(request.getBasePrice());
        product.setComparePrice(request.getComparePrice());
        product.setCostPrice(request.getCostPrice());
        product.setTaxRate(request.getTaxRate() != null ? request.getTaxRate() : BigDecimal.ZERO);
        product.setWeight(request.getWeight());
        product.setSeoTitle(request.getSeoTitle());
        product.setSeoDescription(request.getSeoDescription());
        product.setIsActive(request.getIsActive());
        product.setIsFeatured(request.getIsFeatured());
        product.setIsNew(request.getIsNew());
        product.setIsBestSeller(request.getIsBestSeller());

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

        productRepository.save(product);
        return toResponse(product);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

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
                .basePrice(p.getBasePrice())
                .comparePrice(p.getComparePrice())
                .taxRate(p.getTaxRate())
                .isActive(p.getIsActive())
                .isFeatured(p.getIsFeatured())
                .isNew(p.getIsNew())
                .isBestSeller(p.getIsBestSeller())
                .ratingAvg(p.getRatingAvg())
                .ratingCount(p.getRatingCount())
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
                .attributes(v.getAttributes()).imageUrl(v.getImageUrl())
                .isActive(v.getIsActive()).sortOrder(v.getSortOrder())
                .stock(stock).availableStock(available)
                .build();
    }
}
