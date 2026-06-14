package com.ecommerce.nivora.admin.controller;

import com.ecommerce.nivora.admin.entity.ImportJob;
import com.ecommerce.nivora.admin.service.ImportService;
import com.ecommerce.nivora.admin.service.ProgressTrackingService;
import com.ecommerce.nivora.category.entity.Category;
import com.ecommerce.nivora.category.repository.CategoryRepository;
import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.common.entity.ImportReferenceMapping;
import com.ecommerce.nivora.common.repository.ImportReferenceMappingRepository;
import com.ecommerce.nivora.product.dto.request.ProductRequest;
import com.ecommerce.nivora.category.dto.request.CategoryRequest;
import com.ecommerce.nivora.product.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/import")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Import", description = "Admin excel imports and schema resolution")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class AdminImportController {

    private final ImportService importService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ImportReferenceMappingRepository mappingRepository;
    private final ProgressTrackingService progressTrackingService;

    @GetMapping("/schema/product")
    @Operation(summary = "Get product schema fields dynamically")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductSchema() {
        List<String> fields = getFields(ProductRequest.class);
        fields.remove("variants");
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "entity", "product",
                "fields", fields
        )));
    }

    @GetMapping("/schema/category")
    @Operation(summary = "Get category schema fields dynamically")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategorySchema() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "entity", "category",
                "fields", getFields(CategoryRequest.class)
        )));
    }

    @GetMapping("/categories/resolve")
    @Operation(summary = "Resolve category ID by slug or name (used by FastAPI / Admin)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveCategory(
            @RequestParam(required = false) String slug,
            @RequestParam(required = false) String name) {
        Optional<Category> category = Optional.empty();
        if (slug != null && !slug.isBlank()) {
            category = categoryRepository.findBySlugAndDeletedAtIsNull(slug);
        } else if (name != null && !name.isBlank()) {
            category = categoryRepository.findByNameAndDeletedAtIsNull(name);
        }
        if (category.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("found", true, "id", category.get().getId())));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("found", false)));
    }

    @GetMapping("/brands/resolve")
    @Operation(summary = "Resolve brand existence in DB (used by FastAPI / Admin)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveBrand(@RequestParam String name) {
        boolean exists = productRepository.existsByBrandIgnoreCaseAndDeletedAtIsNull(name);
        return ResponseEntity.ok(ApiResponse.success(Map.of("found", exists, "name", name)));
    }

    @GetMapping("/mappings/resolve")
    @Operation(summary = "Resolve database ID from external ID mapping (used by FastAPI / Admin)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveMapping(
            @RequestParam String entityType,
            @RequestParam String sourceSystem,
            @RequestParam String sourceId) {
        Optional<ImportReferenceMapping> mapping = mappingRepository
                .findByEntityTypeAndSourceSystemAndSourceId(entityType, sourceSystem, sourceId);
        if (mapping.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("found", true, "dbId", mapping.get().getDbId())));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("found", false)));
    }

    @PostMapping("/products/preview")
    @Operation(summary = "Upload product excel and get dry-run preview and mappings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewProducts(
            @RequestParam("file") MultipartFile file, Principal principal) {
        String username = principal != null ? principal.getName() : "admin";
        return ResponseEntity.ok(ApiResponse.success("Excel parsed successfully", importService.previewProducts(file, username)));
    }

    @PostMapping("/categories/preview")
    @Operation(summary = "Upload category excel and get dry-run preview and mappings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewCategories(
            @RequestParam("file") MultipartFile file, Principal principal) {
        String username = principal != null ? principal.getName() : "admin";
        return ResponseEntity.ok(ApiResponse.success("Excel parsed successfully", importService.previewCategories(file, username)));
    }

    @PostMapping("/products/confirm")
    @Operation(summary = "Confirm product import by execution of parsed rows")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmProducts(
            @RequestBody Map<String, Object> body, Principal principal) {
        Long jobId = Long.valueOf(String.valueOf(body.get("jobId")));
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        String username = principal != null ? principal.getName() : "admin";
        return ResponseEntity.ok(ApiResponse.success("Import completed", importService.confirmProductsImport(jobId, rows, username)));
    }

    @PostMapping("/categories/confirm")
    @Operation(summary = "Confirm category import by execution of parsed rows")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmCategories(
            @RequestBody Map<String, Object> body, Principal principal) {
        Long jobId = Long.valueOf(String.valueOf(body.get("jobId")));
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        String username = principal != null ? principal.getName() : "admin";
        return ResponseEntity.ok(ApiResponse.success("Import completed", importService.confirmCategoriesImport(jobId, rows, username)));
    }

    @GetMapping("/jobs")
    @Operation(summary = "Get list of import history jobs with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ImportJob>>> getJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(importService.getImportHistory(PageRequest.of(page, size)))));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get details/status of a specific import job")
    public ResponseEntity<ApiResponse<ImportJob>> getJobDetails(@PathVariable Long jobId) {
        ImportJob job = progressTrackingService.getJobStatus(jobId);
        if (job == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy import job: " + jobId));
        }
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    @GetMapping(value = "/jobs/{jobId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Get real-time SSE progress stream of a specific import job")
    public SseEmitter getJobProgressStream(@PathVariable Long jobId) {
        return progressTrackingService.registerEmitter(jobId);
    }

    private List<String> getFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(Field::getName)
                .collect(Collectors.toList());
    }
}
