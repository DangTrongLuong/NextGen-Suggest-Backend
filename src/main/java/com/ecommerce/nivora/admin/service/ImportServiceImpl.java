package com.ecommerce.nivora.admin.service;

import com.ecommerce.nivora.admin.entity.ImportJob;
import com.ecommerce.nivora.admin.repository.ImportJobRepository;
import com.ecommerce.nivora.category.dto.request.CategoryRequest;
import com.ecommerce.nivora.category.entity.Category;
import com.ecommerce.nivora.category.repository.CategoryRepository;
import com.ecommerce.nivora.category.service.CategoryService;
import com.ecommerce.nivora.common.entity.ImportReferenceMapping;
import com.ecommerce.nivora.common.repository.ImportReferenceMappingRepository;
import com.ecommerce.nivora.product.dto.request.ProductRequest;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.product.repository.ProductRepository;
import com.ecommerce.nivora.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportServiceImpl implements ImportService {

    private final ImportJobRepository jobRepository;
    private final ImportReferenceMappingRepository mappingRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final RestTemplate restTemplate;
    private final PlatformTransactionManager transactionManager;
    private final jakarta.servlet.http.HttpServletRequest httpServletRequest;

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @Override
    public Map<String, Object> previewProducts(MultipartFile file, String username) {
        ImportJob job = ImportJob.builder()
                .type("PRODUCT")
                .filename(file.getOriginalFilename())
                .status("PENDING")
                .createdBy(username)
                .build();
        job = jobRepository.save(job);

        try {
            Map<String, Object> fastApiResponse = forwardFileToFastApi(file, "/api/v1/import/parse/products");
            
            // Extract stats
            int total = (Integer) fastApiResponse.getOrDefault("totalRows", 0);
            int valid = (Integer) fastApiResponse.getOrDefault("validRows", 0);
            int errorsCount = (Integer) fastApiResponse.getOrDefault("errorRows", 0);
            
            job.setTotalRows(total);
            jobRepository.save(job);

            Map<String, Object> result = new HashMap<>(fastApiResponse);
            result.put("jobId", job.getId());
            return result;
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);
            throw new RuntimeException("Lỗi phân tích file: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> previewCategories(MultipartFile file, String username) {
        ImportJob job = ImportJob.builder()
                .type("CATEGORY")
                .filename(file.getOriginalFilename())
                .status("PENDING")
                .createdBy(username)
                .build();
        job = jobRepository.save(job);

        try {
            Map<String, Object> fastApiResponse = forwardFileToFastApi(file, "/api/v1/import/parse/categories");
            
            int total = (Integer) fastApiResponse.getOrDefault("totalRows", 0);
            job.setTotalRows(total);
            jobRepository.save(job);

            Map<String, Object> result = new HashMap<>(fastApiResponse);
            result.put("jobId", job.getId());
            return result;
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);
            throw new RuntimeException("Lỗi phân tích file: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> confirmProductsImport(Long jobId, List<Map<String, Object>> rows, String username) {
        ImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Import job không tồn tại: " + jobId));

        job.setStatus("PROCESSING");
        jobRepository.save(job);

        List<Map<String, Object>> errors = new ArrayList<>();
        int batchSize = 100;
        int successCount = 0;
        int failedCount = 0;

        List<Map<String, Object>> currentBatch = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            currentBatch.add(rows.get(i));
            if (currentBatch.size() == batchSize || i == rows.size() - 1) {
                // Execute batch
                BatchResult batchResult = executeBatch(currentBatch, "PRODUCT");
                successCount += batchResult.successCount;
                failedCount += batchResult.failedCount;
                errors.addAll(batchResult.errors);
                
                // Update job progress
                job.setSuccessRows(successCount);
                job.setFailedRows(failedCount);
                jobRepository.save(job);

                currentBatch.clear();
            }
        }

        job.setStatus(failedCount == 0 ? "COMPLETED" : (successCount > 0 ? "COMPLETED" : "FAILED"));
        job.setFinishedAt(LocalDateTime.now());
        jobRepository.save(job);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("totalRows", rows.size());
        response.put("successRows", successCount);
        response.put("failedRows", failedCount);
        response.put("errors", errors);
        return response;
    }

    @Override
    public Map<String, Object> confirmCategoriesImport(Long jobId, List<Map<String, Object>> rows, String username) {
        ImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Import job không tồn tại: " + jobId));

        job.setStatus("PROCESSING");
        jobRepository.save(job);

        List<Map<String, Object>> errors = new ArrayList<>();
        int batchSize = 100;
        int successCount = 0;
        int failedCount = 0;

        List<Map<String, Object>> currentBatch = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            currentBatch.add(rows.get(i));
            if (currentBatch.size() == batchSize || i == rows.size() - 1) {
                // Execute batch
                BatchResult batchResult = executeBatch(currentBatch, "CATEGORY");
                successCount += batchResult.successCount;
                failedCount += batchResult.failedCount;
                errors.addAll(batchResult.errors);

                // Update job progress
                job.setSuccessRows(successCount);
                job.setFailedRows(failedCount);
                jobRepository.save(job);

                currentBatch.clear();
            }
        }

        job.setStatus(failedCount == 0 ? "COMPLETED" : (successCount > 0 ? "COMPLETED" : "FAILED"));
        job.setFinishedAt(LocalDateTime.now());
        jobRepository.save(job);

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("totalRows", rows.size());
        response.put("successRows", successCount);
        response.put("failedRows", failedCount);
        response.put("errors", errors);
        return response;
    }

    @Override
    public Page<ImportJob> getImportHistory(Pageable pageable) {
        return jobRepository.findAllByOrderByStartedAtDesc(pageable);
    }

    // ── Batch Execution with Fallback ──────────────────────────────────────────

    private static class BatchResult {
        int successCount = 0;
        int failedCount = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
    }

    private BatchResult executeBatch(List<Map<String, Object>> batch, String entityType) {
        BatchResult result = new BatchResult();
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            // Try committing whole batch together
            template.executeWithoutResult(status -> {
                for (Map<String, Object> row : batch) {
                    saveRow(row, entityType);
                }
            });
            result.successCount = batch.size();
        } catch (Exception e) {
            log.warn("Batch import failed, falling back to line-by-line processing. Error: {}", e.getMessage());
            // Fallback to individual rows
            for (Map<String, Object> row : batch) {
                try {
                    template.executeWithoutResult(status -> {
                        saveRow(row, entityType);
                    });
                    result.successCount++;
                } catch (Exception ex) {
                    result.failedCount++;
                    int rowNum = (Integer) row.getOrDefault("rowIndex", 0);
                    Map<String, Object> err = new HashMap<>();
                    err.put("row", rowNum);
                    err.put("reason", ex.getMessage());
                    result.errors.add(err);
                }
            }
        }
        return result;
    }

    private void saveRow(Map<String, Object> row, String entityType) {
        if ("PRODUCT".equals(entityType)) {
            saveProductRow(row);
        } else {
            saveCategoryRow(row);
        }
    }

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

    private void saveCategoryRow(Map<String, Object> row) {
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
            parentId = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("CATEGORY", "Excel", pExternalId)
                    .map(ImportReferenceMapping::getDbId).orElse(null);
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
                .sortOrder(displayOrder)
                .isActive(true)
                .build();

        // 2. Duplicate Detection & Upsert
        Long dbId = null;
        // Case A: external_source_id mapping exists
        if (externalId != null && !externalId.isBlank() && !"null".equals(externalId)) {
            dbId = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("CATEGORY", "Excel", externalId)
                    .map(ImportReferenceMapping::getDbId).orElse(null);
        }
        // Case B: slug exists
        if (dbId == null && slug != null && !slug.isBlank()) {
            dbId = categoryRepository.findBySlugAndDeletedAtIsNull(slug).map(Category::getId).orElse(null);
        }
        // Case C: name exists
        if (dbId == null && name != null && !name.isBlank()) {
            dbId = categoryRepository.findByNameAndDeletedAtIsNull(name).map(Category::getId).orElse(null);
        }

        com.ecommerce.nivora.category.dto.response.CategoryResponse response;
        if (dbId != null) {
            response = categoryService.update(dbId, request);
        } else {
            response = categoryService.create(request);
        }

        // 3. Save mapping
        if (externalId != null && !externalId.isBlank() && !"null".equals(externalId)) {
            ImportReferenceMapping mapping = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("CATEGORY", "Excel", externalId)
                    .orElseGet(() -> ImportReferenceMapping.builder()
                            .entityType("CATEGORY")
                            .sourceSystem("Excel")
                            .sourceId(externalId)
                            .build());
            mapping.setDbId(response.getId());
            mappingRepository.save(mapping);
        }
    }

    private void saveProductRow(Map<String, Object> row) {
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
            categoryId = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("CATEGORY", "Excel", cExternalId)
                    .map(ImportReferenceMapping::getDbId).orElse(null);
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

        // Create variants payload (Single variant matching standard manual DTO)
        ProductRequest.VariantRequest variantRequest = ProductRequest.VariantRequest.builder()
                .sku(sku)
                .name(name)
                .price(basePrice)
                .comparePrice(comparePrice)
                .color(getString(row, "color"))
                .size(getString(row, "size"))
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
                .color(getString(row, "color"))
                .size(getString(row, "size"))
                .sku(sku)
                .variants(List.of(variantRequest))
                .isActive(true)
                .build();

        // 3. Duplicate Detection & Upsert
        Long dbId = null;
        // Priority 1: external_source_id mapping
        if (externalId != null && !externalId.isBlank() && !"null".equals(externalId)) {
            dbId = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("PRODUCT", "Excel", externalId)
                    .map(ImportReferenceMapping::getDbId).orElse(null);
        }
        // Priority 2: SKU
        if (dbId == null && sku != null && !sku.isBlank()) {
            dbId = productRepository.findBySkuAndDeletedAtIsNull(sku).map(Product::getId).orElse(null);
        }
        // Priority 3: Product URL
        if (dbId == null && productUrl != null && !productUrl.isBlank()) {
            dbId = productRepository.findByProductUrlAndDeletedAtIsNull(productUrl).map(Product::getId).orElse(null);
        }
        // Priority 4: Name + Brand
        if (dbId == null && name != null && brand != null && !name.isBlank() && !brand.isBlank()) {
            List<Product> matches = productRepository.findByNameIgnoreCaseAndBrandIgnoreCaseAndDeletedAtIsNull(name, brand);
            if (!matches.isEmpty()) {
                dbId = matches.get(0).getId();
            }
        }

        com.ecommerce.nivora.product.dto.response.ProductResponse response;
        if (dbId != null) {
            response = productService.update(dbId, request);
        } else {
            response = productService.create(request);
        }

        // 4. Save mapping
        if (externalId != null && !externalId.isBlank() && !"null".equals(externalId)) {
            ImportReferenceMapping mapping = mappingRepository.findByEntityTypeAndSourceSystemAndSourceId("PRODUCT", "Excel", externalId)
                    .orElseGet(() -> ImportReferenceMapping.builder()
                            .entityType("PRODUCT")
                            .sourceSystem("Excel")
                            .sourceId(externalId)
                            .build());
            mapping.setDbId(response.getId());
            mappingRepository.save(mapping);
        }
    }

    // ── Helper method to forward file ──────────────────────────────────────────

    private Map<String, Object> forwardFileToFastApi(MultipartFile file, String path) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Token Relay: Forward admin authentication header
            String authHeader = httpServletRequest.getHeader("Authorization");
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String url = fastApiUrl + path;

            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to connect to FastAPI endpoint: {}", path, e);
            throw new RuntimeException("Lỗi kết nối tới FastAPI: " + e.getMessage(), e);
        }
    }
}
