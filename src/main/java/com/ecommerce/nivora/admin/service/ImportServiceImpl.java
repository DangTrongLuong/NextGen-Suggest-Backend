package com.ecommerce.nivora.admin.service;

import com.ecommerce.nivora.admin.entity.ImportJob;
import com.ecommerce.nivora.admin.repository.ImportJobRepository;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Qualifier;
import com.ecommerce.nivora.product.entity.Product;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.core.task.TaskExecutor;

@Service
@Slf4j
public class ImportServiceImpl implements ImportService {

    private final ImportJobRepository jobRepository;
    private final RestTemplate restTemplate;
    private final jakarta.servlet.http.HttpServletRequest httpServletRequest;

    private final ProgressTrackingService progressTrackingService;
    private final ImportMySQLService importMySQLService;
    private final ImportEmbeddingService importEmbeddingService;
    private final ImportQdrantService importQdrantService;
    private final TaskExecutor taskExecutor;

    public ImportServiceImpl(
            ImportJobRepository jobRepository,
            RestTemplate restTemplate,
            jakarta.servlet.http.HttpServletRequest httpServletRequest,
            ProgressTrackingService progressTrackingService,
            ImportMySQLService importMySQLService,
            ImportEmbeddingService importEmbeddingService,
            ImportQdrantService importQdrantService,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.jobRepository = jobRepository;
        this.restTemplate = restTemplate;
        this.httpServletRequest = httpServletRequest;
        this.progressTrackingService = progressTrackingService;
        this.importMySQLService = importMySQLService;
        this.importEmbeddingService = importEmbeddingService;
        this.importQdrantService = importQdrantService;
        this.taskExecutor = taskExecutor;
    }

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

        log.info("Starting asynchronous Products Import Job with high-performance batching. Job ID: {}, Rows to process: {}", jobId, rows.size());

        taskExecutor.execute(() -> {
            int successCount = 0;
            int duplicateCount = 0;
            int failedCount = 0;
            List<Map<String, Object>> duplicateDetails = new ArrayList<>();
            List<Map<String, Object>> errorDetails = new ArrayList<>();

            List<Product> batchProducts = new ArrayList<>();
            Map<String, Integer> uuidToRowNum = new HashMap<>();
            Map<String, String> uuidToName = new HashMap<>();

            int processedInLoop = 0;
            for (Map<String, Object> row : rows) {
                row.put("jobId", jobId);
                processedInLoop++;
                boolean isLastRow = (processedInLoop == rows.size());
                int rowNum = (Integer) row.getOrDefault("rowIndex", 0);
                String name = (String) row.get("name");
                if (name == null) {
                    name = (String) row.get("product_name");
                }
                if (name == null) {
                    name = "Dòng #" + rowNum;
                }

                try {
                    // Check duplicate
                    if (importMySQLService.isProductDuplicate(row)) {
                        duplicateCount++;
                        duplicateDetails.add(Map.of(
                                "row", rowNum,
                                "item", name,
                                "reason", "Sản phẩm đã tồn tại trong hệ thống (trùng SKU, URL hoặc Tên + Thương hiệu)"
                        ));
                        // Push progress immediately for skipped duplicate
                        progressTrackingService.updateProgress(jobId, successCount, duplicateCount, failedCount, "PROCESSING", duplicateDetails, errorDetails);
                    } else {
                        // Pipeline Step 1, 2, 4: Save to MySQL (committed in separate REQUIRES_NEW transaction)
                        var product = importMySQLService.saveProductRow(row);
                        batchProducts.add(product);
                        uuidToRowNum.put(product.getUuid(), rowNum);
                        uuidToName.put(product.getUuid(), name);
                    }
                } catch (Exception e) {
                    log.error("MySQL Insert failed for row: {}", rowNum, e);
                    failedCount++;
                    errorDetails.add(Map.of(
                            "row", rowNum,
                            "item", name,
                            "reason", "Lỗi lưu MySQL: " + e.getMessage()
                    ));
                    progressTrackingService.updateProgress(jobId, successCount, duplicateCount, failedCount, "PROCESSING", duplicateDetails, errorDetails);
                }

                // If batch size is reached or we are on the last row and have pending products
                if (batchProducts.size() >= 50 || (isLastRow && !batchProducts.isEmpty())) {
                    List<String> uuids = batchProducts.stream().map(Product::getUuid).collect(Collectors.toList());
                    try {
                        // Batch index in FastAPI (Embedding + Qdrant)
                        for (Product p : batchProducts) {
                            importEmbeddingService.logEmbeddingCreation(p.getUuid(), p.getName());
                        }
                        importQdrantService.syncProducts(uuids);
                        
                        successCount += batchProducts.size();
                        progressTrackingService.updateProgress(jobId, successCount, duplicateCount, failedCount, "PROCESSING", duplicateDetails, errorDetails);
                    } catch (Exception ex) {
                        log.warn("Batch Qdrant indexing failed for {} products, falling back to individual indexing. Error: {}", batchProducts.size(), ex.getMessage());
                        // Fallback to individual indexing
                        for (Product p : batchProducts) {
                            String uuid = p.getUuid();
                            Integer origRowNum = uuidToRowNum.get(uuid);
                            String origName = uuidToName.get(uuid);
                            try {
                                importQdrantService.syncProduct(uuid);
                                successCount++;
                            } catch (Exception individualEx) {
                                log.error("Individual indexing failed during fallback for product UUID: {}. Rolling back MySQL insert.", uuid, individualEx);
                                // Roll back MySQL insert
                                importMySQLService.deleteProductByUuid(uuid);
                                failedCount++;
                                errorDetails.add(Map.of(
                                        "row", origRowNum,
                                        "item", origName,
                                        "reason", "Lỗi tạo vector/Qdrant: " + individualEx.getMessage()
                                ));
                            }
                            // Push progress for each fallback retry
                            progressTrackingService.updateProgress(jobId, successCount, duplicateCount, failedCount, "PROCESSING", duplicateDetails, errorDetails);
                        }
                    }
                    
                    // Clear batch
                    batchProducts.clear();
                    uuidToRowNum.clear();
                    uuidToName.clear();

                    // Optional: tiny sleep to keep progress updates smooth
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Background import thread was interrupted");
                        break;
                    }
                }
            }

            // Finish
            log.info("Finished Products Import Job. Job ID: {}, Success: {}, Duplicate: {}, Failed: {}", jobId, successCount, duplicateCount, failedCount);
            progressTrackingService.completeJob(jobId, successCount, duplicateCount, failedCount, duplicateDetails, errorDetails);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("status", "PROCESSING");
        response.put("totalRows", rows.size());
        return response;
    }

    @Override
    public Map<String, Object> confirmCategoriesImport(Long jobId, List<Map<String, Object>> rows, String username) {
        ImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Import job không tồn tại: " + jobId));

        job.setStatus("PROCESSING");
        jobRepository.save(job);

        log.info("Starting asynchronous Categories Import Job. Job ID: {}, Rows to process: {}", jobId, rows.size());

        taskExecutor.execute(() -> {
            int successCount = 0;
            int duplicateCount = 0;
            int failedCount = 0;
            List<Map<String, Object>> duplicateDetails = new ArrayList<>();
            List<Map<String, Object>> errorDetails = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                row.put("jobId", jobId);
                int rowNum = (Integer) row.getOrDefault("rowIndex", 0);
                String name = (String) row.get("name");
                if (name == null) {
                    name = (String) row.get("category_name");
                }
                if (name == null) {
                    name = "Dòng #" + rowNum;
                }

                try {
                    // Check duplicate
                    if (importMySQLService.isCategoryDuplicate(row)) {
                        duplicateCount++;
                        duplicateDetails.add(Map.of(
                                "row", rowNum,
                                "item", name,
                                "reason", "Danh mục đã tồn tại trong hệ thống (trùng Tên hoặc Slug)"
                        ));
                    } else {
                        // Save Category
                        importMySQLService.saveCategoryRow(row);
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("Category Insert failed for row: {}", rowNum, e);
                    failedCount++;
                    errorDetails.add(Map.of(
                            "row", rowNum,
                            "item", name,
                            "reason", "Lỗi lưu MySQL: " + e.getMessage()
                    ));
                }

                // Push progress
                progressTrackingService.updateProgress(jobId, successCount, duplicateCount, failedCount, "PROCESSING", duplicateDetails, errorDetails);

                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Background import thread was interrupted");
                    break;
                }
            }

            // Finish
            log.info("Finished Categories Import Job. Job ID: {}, Success: {}, Duplicate: {}, Failed: {}", jobId, successCount, duplicateCount, failedCount);
            progressTrackingService.completeJob(jobId, successCount, duplicateCount, failedCount, duplicateDetails, errorDetails);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("status", "PROCESSING");
        response.put("totalRows", rows.size());
        return response;
    }

    @Override
    public Page<ImportJob> getImportHistory(Pageable pageable) {
        return jobRepository.findAllByOrderByStartedAtDesc(pageable);
    }

    // ── Helper method to forward file ──────────────────────────────────────────

    private Map<String, Object> forwardFileToFastApi(MultipartFile file, String path) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

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
