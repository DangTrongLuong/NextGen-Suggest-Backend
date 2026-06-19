package com.ecommerce.nivora.admin.service;

import com.ecommerce.nivora.admin.entity.ImportJob;
import com.ecommerce.nivora.category.dto.request.CategoryRequest;
import com.ecommerce.nivora.category.entity.Category;
import com.ecommerce.nivora.category.entity.CategoryMain;
import com.ecommerce.nivora.category.repository.CategoryRepository;
import com.ecommerce.nivora.category.repository.CategoryMainRepository;
import com.ecommerce.nivora.category.service.CategoryService;
import com.ecommerce.nivora.common.entity.ImportReferenceMapping;
import com.ecommerce.nivora.common.repository.ImportReferenceMappingRepository;
import com.ecommerce.nivora.product.dto.request.ProductRequest;
import com.ecommerce.nivora.common.utils.SlugUtils;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.product.repository.ProductRepository;
import com.ecommerce.nivora.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportMySQLServiceImpl implements ImportMySQLService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryMainRepository categoryMainRepository;
    private final ImportReferenceMappingRepository mappingRepository;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final SlugUtils slugUtils;

    @Override
    @Transactional(readOnly = true)
    public boolean isProductDuplicate(Map<String, Object> row) {
        String name = getString(row, "name", "product_name");
        String brand = getString(row, "brand");
        String productUrl = getString(row, "productUrl", "product_url");
        String externalId = getString(row, "product_id", "id");
        String sku = getString(row, "sku");

        // Priority 1: external_source_id mapping
        if (externalId != null && !externalId.isBlank() && !"null".equals(externalId)) {
            boolean mappingExists = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("PRODUCT", getSourceSystem(row), externalId).isPresent();
            if (mappingExists) return true;
        }
        // Priority 2: SKU
        if (sku != null && !sku.isBlank()) {
            boolean skuExists = productRepository.findBySkuAndDeletedAtIsNull(sku).isPresent();
            if (skuExists) return true;
        }
        // Priority 3: Product URL
        if (productUrl != null && !productUrl.isBlank()) {
            boolean urlExists = productRepository.findByProductUrlAndDeletedAtIsNull(productUrl).isPresent();
            if (urlExists) return true;
        }
        // Priority 4: Name + Brand
        if (name != null && brand != null && !name.isBlank() && !brand.isBlank()) {
            boolean nameBrandExists = !productRepository.findByNameIgnoreCaseAndBrandIgnoreCaseAndDeletedAtIsNull(name, brand).isEmpty();
            if (nameBrandExists) return true;
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCategoryDuplicate(Map<String, Object> row) {
        String name = getString(row, "name", "category_name");
        String slug = getString(row, "slug");
        String externalId = getString(row, "category_id", "id");

        // Case A: external_source_id mapping exists
        if (externalId != null && !externalId.isBlank() && !"null".equals(externalId)) {
            boolean mappingExists = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("CATEGORY", getSourceSystem(row), externalId).isPresent();
            if (mappingExists) return true;
        }
        // Case B: slug exists
        if (slug != null && !slug.isBlank()) {
            boolean slugExists = categoryRepository.findBySlugAndDeletedAtIsNull(slug).isPresent();
            if (slugExists) return true;
        }
        // Case C: name exists
        if (name != null && !name.isBlank()) {
            boolean nameExists = categoryRepository.findByNameAndDeletedAtIsNull(name).isPresent();
            if (nameExists) return true;
        }

        return false;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Product saveProductRow(Map<String, Object> row) {
        String name = getString(row, "name", "product_name");
        String description = getString(row, "description");
        String brand = getString(row, "brand");
        String productUrl = getString(row, "productUrl", "product_url");
        String externalId = getString(row, "product_id", "id");

        // Base price
        BigDecimal basePrice = BigDecimal.ZERO;
        Object priceObj = getObject(row, "basePrice", "price");
        if (priceObj instanceof Number) {
            basePrice = BigDecimal.valueOf(((Number) priceObj).doubleValue());
        } else if (priceObj instanceof String) {
            try { basePrice = new BigDecimal((String) priceObj); } catch (Exception ignored) {}
        }

        // Compare price (original_price)
        BigDecimal comparePrice = null;
        Object comparePriceObj = getObject(row, "comparePrice", "original_price");
        if (comparePriceObj instanceof Number) {
            comparePrice = BigDecimal.valueOf(((Number) comparePriceObj).doubleValue());
        } else if (comparePriceObj instanceof String) {
            try { comparePrice = new BigDecimal((String) comparePriceObj); } catch (Exception ignored) {}
        }

        // Stock
        int stock = 0;
        Object stockObj = getObject(row, "stock", "quantity");
        if (stockObj instanceof Number) {
            stock = ((Number) stockObj).intValue();
        } else if (stockObj instanceof String) {
            try { stock = Integer.parseInt((String) stockObj); } catch (Exception ignored) {}
        }

        // Category resolution
        Long categoryId = null;
        String cSlug = getString(row, "category_slug", "categorySlug");
        String cName = getString(row, "category_name", "categoryName");
        String cExternalId = getString(row, "category_id");

        if (cSlug != null && !cSlug.isBlank()) {
            categoryId = categoryRepository.findBySlugAndDeletedAtIsNull(cSlug)
                    .map(Category::getId).orElse(null);
        }
        if (categoryId == null && cName != null && !cName.isBlank()) {
            categoryId = categoryRepository.findByNameAndDeletedAtIsNull(cName)
                    .map(Category::getId).orElse(null);
        }
        if (categoryId == null && cExternalId != null && !cExternalId.isBlank() && !"null".equals(cExternalId)) {
            categoryId = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("CATEGORY", getSourceSystem(row), cExternalId)
                    .map(ImportReferenceMapping::getDbId)
                    .orElseGet(() -> {
                        List<ImportReferenceMapping> mappings = mappingRepository.findByEntityTypeAndSourceIdOrderByIdDesc("CATEGORY", cExternalId);
                        return mappings.isEmpty() ? null : mappings.get(0).getDbId();
                    });
        }

        if (categoryId == null) {
            throw new NoSuchElementException("Không thể phân giải danh mục cho sản phẩm: " + name);
        }

        // Images list parsing
        String rawImageUrl = getString(row, "imageUrl", "thumbnail_url");
        List<String> parsedThumbnailUrls = parseImageUrls(rawImageUrl);

        Object imagesObj = getObject(row, "imageUrls", "image_urls");
        List<String> parsedGalleryUrls = parseImageUrls(imagesObj);

        List<String> imageUrlsList = new ArrayList<>();
        for (String url : parsedThumbnailUrls) {
            if (!imageUrlsList.contains(url)) {
                imageUrlsList.add(url);
            }
        }
        for (String url : parsedGalleryUrls) {
            if (!imageUrlsList.contains(url)) {
                imageUrlsList.add(url);
            }
        }

        String imageUrl = null;
        if (!parsedThumbnailUrls.isEmpty()) {
            imageUrl = parsedThumbnailUrls.get(0);
        } else if (!imageUrlsList.isEmpty()) {
            imageUrl = imageUrlsList.get(0);
        }

        // SKU generation
        String sku = getString(row, "sku");
        if (sku == null || sku.isBlank()) {
            sku = "PRODUCT-" + System.nanoTime();
        }

        String rawColor = cleanColorOrSize(getString(row, "color"));
        String rawSize = cleanColorOrSize(getString(row, "size"));

        // Create variants payload (Single variant matching standard manual DTO)
        ProductRequest.VariantRequest variantRequest = ProductRequest.VariantRequest.builder()
                .sku(sku)
                .name(name)
                .price(basePrice)
                .comparePrice(comparePrice)
                .color(rawColor)
                .size(rawSize)
                .stock(stock)
                .isActive(true)
                .imageUrl(imageUrl)
                .build();

        ProductRequest request = ProductRequest.builder()
                .name(name)
                .description(description)
                .brand(brand)
                .productUrl(productUrl)
                .categoryId(categoryId)
                .basePrice(basePrice)
                .comparePrice(comparePrice)
                .stock(stock)
                .imageUrl(imageUrl)
                .imageUrls(imageUrlsList)
                .color(rawColor)
                .size(rawSize)
                .sku(sku)
                .variants(List.of(variantRequest))
                .isActive(true)
                .build();

        // Always create a new product since we filtered duplicates beforehand
        var response = productService.create(request);
        Product product = productRepository.findById(response.getId())
                .orElseThrow(() -> new IllegalStateException("Product not found after save"));

        // Save mapping
        if (externalId != null && !externalId.isBlank() && !"null".equals(externalId)) {
            ImportReferenceMapping mapping = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("PRODUCT", getSourceSystem(row), externalId)
                    .orElseGet(() -> ImportReferenceMapping.builder()
                            .entityType("PRODUCT")
                            .sourceSystem(getSourceSystem(row))
                            .sourceId(externalId)
                            .build());
            mapping.setDbId(response.getId());
            mappingRepository.save(mapping);
        }

        return product;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Category saveCategoryRow(Map<String, Object> row) {
        String name = getString(row, "name", "category_name");
        String slug = getString(row, "slug");
        String description = getString(row, "description");
        String imageUrl = getString(row, "imageUrl", "category_image_url");
        String externalId = getString(row, "category_id", "id");

        // 1. Parent resolution
        Long parentId = null;
        String pSlug = getString(row, "parent_slug", "parentSlug");
        String pName = getString(row, "parent_name", "parentName");
        String pExternalId = getString(row, "parent_id", "parentId");

        if (pSlug != null && !pSlug.isBlank()) {
            parentId = categoryRepository.findBySlugAndDeletedAtIsNull(pSlug)
                    .map(Category::getId).orElse(null);
        }
        if (parentId == null && pName != null && !pName.isBlank()) {
            parentId = categoryRepository.findByNameAndDeletedAtIsNull(pName)
                    .map(Category::getId).orElse(null);
        }
        if (parentId == null && pExternalId != null && !pExternalId.isBlank() && !"null".equals(pExternalId)) {
            parentId = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("CATEGORY", getSourceSystem(row), pExternalId)
                    .map(ImportReferenceMapping::getDbId)
                    .orElseGet(() -> {
                        List<ImportReferenceMapping> mappings = mappingRepository.findByEntityTypeAndSourceIdOrderByIdDesc("CATEGORY", pExternalId);
                        return mappings.isEmpty() ? null : mappings.get(0).getDbId();
                    });
        }

        // CategoryMain resolution
        Long categoryMainId = null;
        String mainName = getString(row, "category_main", "categoryMain");
        if (mainName != null && !mainName.trim().isEmpty()) {
            CategoryMain categoryMain = categoryMainRepository.findByNameAndDeletedAtIsNull(mainName.trim())
                    .orElseGet(() -> {
                        String newSlug = slugUtils.uniqueSlug(mainName.trim(), categoryMainRepository::existsBySlug);
                        CategoryMain newCM = CategoryMain.builder()
                                .name(mainName.trim())
                                .slug(newSlug)
                                .build();
                        return categoryMainRepository.save(newCM);
                    });
            categoryMainId = categoryMain.getId();
        }

        // Convert display order
        int displayOrder = 0;
        Object orderObj = getObject(row, "sortOrder", "display_order");
        if (orderObj instanceof Number) {
            displayOrder = ((Number) orderObj).intValue();
        } else if (orderObj instanceof String) {
            try {
                displayOrder = Integer.parseInt((String) orderObj);
            } catch (Exception ignored) {}
        }

        CategoryRequest request = CategoryRequest.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .imageUrl(imageUrl)
                .parentId(parentId)
                .categoryMainId(categoryMainId)
                .sortOrder(displayOrder)
                .isActive(true)
                .build();

        // Always create a new category since we filtered duplicates beforehand
        var response = categoryService.create(request);
        Category category = categoryRepository.findById(response.getId())
                .orElseThrow(() -> new IllegalStateException("Category not found after save"));

        // Save mapping
        if (externalId != null && !externalId.isBlank() && !"null".equals(externalId)) {
            ImportReferenceMapping mapping = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("CATEGORY", getSourceSystem(row), externalId)
                    .orElseGet(() -> ImportReferenceMapping.builder()
                            .entityType("CATEGORY")
                            .sourceSystem(getSourceSystem(row))
                            .sourceId(externalId)
                            .build());
            mapping.setDbId(response.getId());
            mappingRepository.save(mapping);
        }

        return category;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteProductByUuid(String uuid) {
        productRepository.findByUuidAndDeletedAtIsNull(uuid).ifPresent(product -> {
            log.info("Rolling back product in MySQL due to indexing failure. UUID: {}", uuid);
            
            // Delete reference mappings if any
            mappingRepository.deleteByEntityTypeAndDbId("PRODUCT", product.getId());

            productRepository.delete(product);
            log.info("Successfully deleted product from MySQL. UUID: {}", uuid);
        });
    }

    private String getSourceSystem(Map<String, Object> row) {
        Object jobId = row.get("jobId");
        return jobId != null ? "Excel-Job-" + jobId : "Excel";
    }

    // ── Helper parsing methods ──────────────────────────────────────────

    private String getString(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object val = row.get(key);
            if (val != null) {
                return String.valueOf(val);
            }
        }
        return null;
    }

    private Object getObject(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object val = row.get(key);
            if (val != null) {
                return val;
            }
        }
        return null;
    }

    private List<String> parseImageUrls(Object input) {
        List<String> list = new ArrayList<>();
        if (input == null) {
            return list;
        }
        if (input instanceof List) {
            for (Object item : (List<?>) input) {
                list.addAll(parseImageUrls(item));
            }
        } else {
            String str = String.valueOf(input).trim();
            if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
                str = str.replace("[", "").replace("]", "").replace("\"", "").replace("'", "");
                String[] parts = str.split("[,;|]");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty() && !"null".equalsIgnoreCase(trimmed) && !list.contains(trimmed)) {
                        list.add(trimmed);
                    }
                }
            }
        }
        return list;
    }

    private String cleanColorOrSize(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }
        String cleaned = val.trim();
        // Remove outer brackets if present
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        // Remove quotes
        cleaned = cleaned.replace("\"", "").replace("'", "");
        
        // Split by comma, trim each element, and rejoin with comma
        String[] parts = cleaned.split(",");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(trimmed);
            }
        }
        cleaned = sb.toString();

        // Truncate to maximum 50 characters to prevent database truncation errors
        if (cleaned.length() > 50) {
            int lastComma = cleaned.lastIndexOf(',', 47);
            if (lastComma > 0) {
                cleaned = cleaned.substring(0, lastComma);
            } else {
                cleaned = cleaned.substring(0, 47) + "...";
            }
        }
        return cleaned;
    }
}
