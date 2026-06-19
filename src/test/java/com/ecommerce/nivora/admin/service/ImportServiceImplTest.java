package com.ecommerce.nivora.admin.service;

import com.ecommerce.nivora.admin.entity.ImportJob;
import com.ecommerce.nivora.admin.repository.ImportJobRepository;
import com.ecommerce.nivora.category.entity.Category;
import com.ecommerce.nivora.category.repository.CategoryRepository;
import com.ecommerce.nivora.category.entity.CategoryMain;
import com.ecommerce.nivora.category.repository.CategoryMainRepository;
import com.ecommerce.nivora.common.entity.ImportReferenceMapping;
import com.ecommerce.nivora.common.repository.ImportReferenceMappingRepository;
import com.ecommerce.nivora.product.entity.Product;
import com.ecommerce.nivora.product.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ImportServiceImplTest {

    @Autowired
    private ImportService importService;

    @Autowired
    private ImportJobRepository jobRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryMainRepository categoryMainRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ImportReferenceMappingRepository mappingRepository;

    private Category parentCategory;
    private Category leafCategory;
    private ImportJob productJob;
    private ImportJob categoryJob;

    @BeforeEach
    void setup() {
        // Clean up before test
        cleanup();

        // Create base Category hierarchy
        parentCategory = Category.builder()
                .name("Parent Test Category")
                .slug("parent-test-category")
                .isActive(true)
                .displayOrder(0)
                .build();
        parentCategory = categoryRepository.save(parentCategory);

        leafCategory = Category.builder()
                .name("Leaf Test Category")
                .slug("leaf-test-category")
                .parent(parentCategory)
                .isActive(true)
                .displayOrder(0)
                .build();
        leafCategory = categoryRepository.save(leafCategory);

        productJob = ImportJob.builder()
                .type("PRODUCT")
                .filename("products_test.xlsx")
                .status("PENDING")
                .totalRows(2)
                .createdBy("admin")
                .build();
        productJob = jobRepository.save(productJob);

        categoryJob = ImportJob.builder()
                .type("CATEGORY")
                .filename("categories_test.xlsx")
                .status("PENDING")
                .totalRows(2)
                .createdBy("admin")
                .build();
        categoryJob = jobRepository.save(categoryJob);
    }

    @AfterEach
    void cleanup() {
        // Delete test products
        List<Product> products = productRepository.findByNameIgnoreCaseAndBrandIgnoreCaseAndDeletedAtIsNull("Test Product A", "Test Brand");
        products.addAll(productRepository.findByNameIgnoreCaseAndBrandIgnoreCaseAndDeletedAtIsNull("Test Product B", "Test Brand"));
        products.addAll(productRepository.findByNameIgnoreCaseAndBrandIgnoreCaseAndDeletedAtIsNull("Test Product C", "Test Brand"));
        for (Product p : products) {
            try {
                productRepository.delete(p);
            } catch (Exception ignored) {}
        }

        // Delete test categories by slug
        categoryRepository.findBySlugAndDeletedAtIsNull("new-child-category")
                .ifPresent(c -> {
                    c.setParent(null);
                    categoryRepository.save(c);
                    categoryRepository.delete(c);
                });
        categoryRepository.findBySlugAndDeletedAtIsNull("new-child-category-with-main")
                .ifPresent(c -> {
                    c.setParent(null);
                    categoryRepository.save(c);
                    categoryRepository.delete(c);
                });
        categoryRepository.findBySlugAndDeletedAtIsNull("new-parent-category")
                .ifPresent(categoryRepository::delete);

        categoryRepository.findBySlugAndDeletedAtIsNull("leaf-test-category")
                .ifPresent(c -> {
                    c.setParent(null);
                    categoryRepository.save(c);
                    categoryRepository.delete(c);
                });
        categoryRepository.findBySlugAndDeletedAtIsNull("parent-test-category")
                .ifPresent(categoryRepository::delete);

        // Delete test CategoryMain records
        categoryMainRepository.findByNameAndDeletedAtIsNull("Thời Trang Test")
                .ifPresent(categoryMainRepository::delete);

        // Clean mapping table
        mappingRepository.deleteAll();

        // Clean job history
        jobRepository.deleteAll();
    }

    @Test
    void testConfirmCategoriesImport_Success() {
        // Prepare rows for Category import
        Map<String, Object> parentRow = new HashMap<>();
        parentRow.put("rowIndex", 2);
        parentRow.put("category_id", "ext-cat-parent");
        parentRow.put("name", "New Parent Category");
        parentRow.put("slug", "new-parent-category");
        parentRow.put("description", "Parent description");
        parentRow.put("display_order", 1);

        Map<String, Object> childRow = new HashMap<>();
        childRow.put("rowIndex", 3);
        childRow.put("category_id", "ext-cat-child");
        childRow.put("parent_id", "ext-cat-parent"); // parent resolution via external ID
        childRow.put("name", "New Child Category");
        childRow.put("slug", "new-child-category");
        childRow.put("description", "Child description");
        childRow.put("display_order", 2);

        List<Map<String, Object>> rows = List.of(parentRow, childRow);

        // Execute Confirm Categories Import
        importService.confirmCategoriesImport(categoryJob.getId(), rows, "admin");
        ImportJob finishedJob = waitForJob(categoryJob.getId());

        assertNotNull(finishedJob);
        assertEquals("COMPLETED", finishedJob.getStatus());
        assertEquals(2, finishedJob.getSuccessRows());
        assertEquals(0, finishedJob.getFailedRows());

        // Verify categories created in Database
        Optional<Category> parentOpt = categoryRepository.findBySlugAndDeletedAtIsNull("new-parent-category");
        assertTrue(parentOpt.isPresent());
        assertEquals("New Parent Category", parentOpt.get().getName());

        Optional<Category> childOpt = categoryRepository.findBySlugAndDeletedAtIsNull("new-child-category");
        assertTrue(childOpt.isPresent());
        assertEquals("New Child Category", childOpt.get().getName());
        assertNotNull(childOpt.get().getParent());
        assertEquals(parentOpt.get().getId(), childOpt.get().getParent().getId());

        // Verify reference mappings stored
        Optional<ImportReferenceMapping> parentMapping = mappingRepository
                .findByEntityTypeAndSourceSystemAndSourceId("CATEGORY", "Excel-Job-" + categoryJob.getId(), "ext-cat-parent");
        assertTrue(parentMapping.isPresent());
        assertEquals(parentOpt.get().getId(), parentMapping.get().getDbId());

        Optional<ImportReferenceMapping> childMapping = mappingRepository
                .findByEntityTypeAndSourceSystemAndSourceId("CATEGORY", "Excel-Job-" + categoryJob.getId(), "ext-cat-child");
        assertTrue(childMapping.isPresent());
        assertEquals(childOpt.get().getId(), childMapping.get().getDbId());
    }

    @Test
    void testConfirmProductsImport_SuccessAndDuplicateDetection() {
        // Prepare rows for Product import
        Map<String, Object> prodRow1 = new HashMap<>();
        prodRow1.put("rowIndex", 2);
        prodRow1.put("product_id", "ext-prod-a");
        prodRow1.put("product_name", "Test Product A");
        prodRow1.put("brand", "Test Brand");
        prodRow1.put("description", "Test Product A Description");
        prodRow1.put("price", 150000.00);
        prodRow1.put("original_price", 200000.00);
        prodRow1.put("stock", 50);
        prodRow1.put("sku", "SKU-PROD-A");
        prodRow1.put("category_slug", "leaf-test-category"); // Category resolution via slug

        List<Map<String, Object>> rows = List.of(prodRow1);

        // Execute Product Import
        importService.confirmProductsImport(productJob.getId(), rows, "admin");
        ImportJob finishedJob = waitForJob(productJob.getId());

        assertNotNull(finishedJob);
        assertEquals("COMPLETED", finishedJob.getStatus());
        assertEquals(1, finishedJob.getSuccessRows());
        assertEquals(0, finishedJob.getFailedRows());

        // Verify product exists in database
        Optional<Product> prodOpt = productRepository.findBySkuAndDeletedAtIsNull("SKU-PROD-A");
        assertTrue(prodOpt.isPresent());
        assertEquals("Test Product A", prodOpt.get().getName());
        assertEquals(leafCategory.getId(), prodOpt.get().getCategory().getId());

        // Verify Import Reference Mapping
        Optional<ImportReferenceMapping> refMapping = mappingRepository
                .findByEntityTypeAndSourceSystemAndSourceId("PRODUCT", "Excel-Job-" + productJob.getId(), "ext-prod-a");
        assertTrue(refMapping.isPresent());
        assertEquals(prodOpt.get().getId(), refMapping.get().getDbId());

        // Test Duplicate Detection (Update case):
        // Update price and name using the same external ID "ext-prod-a"
        Map<String, Object> prodRow1Update = new HashMap<>();
        prodRow1Update.put("rowIndex", 3);
        prodRow1Update.put("product_id", "ext-prod-a");
        prodRow1Update.put("product_name", "Test Product A");
        prodRow1Update.put("brand", "Test Brand");
        prodRow1Update.put("description", "Updated Description");
        prodRow1Update.put("price", 180000.00);
        prodRow1Update.put("stock", 60);
        prodRow1Update.put("sku", "SKU-PROD-A");
        prodRow1Update.put("category_slug", "leaf-test-category");

        importService.confirmProductsImport(productJob.getId(), List.of(prodRow1Update), "admin");
        ImportJob finishedUpdateJob = waitForJob(productJob.getId());

        assertNotNull(finishedUpdateJob);
        assertEquals("COMPLETED", finishedUpdateJob.getStatus());
        assertEquals(0, finishedUpdateJob.getSuccessRows());
        assertEquals(1, finishedUpdateJob.getDuplicateRows());
        assertEquals(0, finishedUpdateJob.getFailedRows());

        // Verify database did not change (original product still has original description)
        Optional<Product> updatedProdOpt = productRepository.findBySkuAndDeletedAtIsNull("SKU-PROD-A");
        assertTrue(updatedProdOpt.isPresent());
        assertEquals(prodOpt.get().getId(), updatedProdOpt.get().getId());
        assertEquals("Test Product A Description", updatedProdOpt.get().getDescription());
    }

    @Test
    void testBatchFallbackFaultIsolation() {
        // Create 2 rows: 1 valid, 1 invalid (invalid category resolution will trigger exception)
        Map<String, Object> validRow = new HashMap<>();
        validRow.put("rowIndex", 2);
        validRow.put("product_id", "ext-prod-b");
        validRow.put("product_name", "Test Product B");
        validRow.put("brand", "Test Brand");
        validRow.put("price", 100000.0);
        validRow.put("stock", 10);
        validRow.put("sku", "SKU-PROD-B");
        validRow.put("category_slug", "leaf-test-category");

        Map<String, Object> invalidRow = new HashMap<>();
        invalidRow.put("rowIndex", 3);
        invalidRow.put("product_id", "ext-prod-c");
        invalidRow.put("product_name", "Test Product C");
        invalidRow.put("brand", "Test Brand");
        invalidRow.put("price", 200000.0);
        invalidRow.put("stock", 20);
        invalidRow.put("sku", "SKU-PROD-C");
        invalidRow.put("category_slug", "invalid-non-existing-category"); // Will fail resolution!

        List<Map<String, Object>> rows = List.of(validRow, invalidRow);

        // Execute batch import
        importService.confirmProductsImport(productJob.getId(), rows, "admin");
        ImportJob finishedJob = waitForJob(productJob.getId());

        assertNotNull(finishedJob);
        assertEquals("COMPLETED", finishedJob.getStatus());
        assertEquals(1, finishedJob.getSuccessRows());
        assertEquals(1, finishedJob.getFailedRows());

        // Check the errors list returned
        assertNotNull(finishedJob.getErrorDetails());
        assertTrue(finishedJob.getErrorDetails().contains("Không thể phân giải danh mục"));
        assertTrue(finishedJob.getErrorDetails().contains("\"row\":3"));

        // Verify that the valid product is saved in database
        Optional<Product> validProd = productRepository.findBySkuAndDeletedAtIsNull("SKU-PROD-B");
        assertTrue(validProd.isPresent());
        assertEquals("Test Product B", validProd.get().getName());

        // Verify that the invalid product is NOT saved in database
        Optional<Product> invalidProd = productRepository.findBySkuAndDeletedAtIsNull("SKU-PROD-C");
        assertFalse(invalidProd.isPresent());
    }

    @Test
    void testConfirmCategoriesImport_WithCategoryMain_Success() {
        // Delete first to ensure fresh state
        categoryRepository.findBySlugAndDeletedAtIsNull("new-child-category-with-main")
                .ifPresent(categoryRepository::delete);
        categoryMainRepository.findByNameAndDeletedAtIsNull("Thời Trang Test")
                .ifPresent(categoryMainRepository::delete);

        // Prepare rows for Category import with a category_main column
        Map<String, Object> row = new HashMap<>();
        row.put("rowIndex", 2);
        row.put("category_id", "ext-cat-with-main");
        row.put("name", "New Child Category with Main");
        row.put("slug", "new-child-category-with-main");
        row.put("description", "Category with Main description");
        row.put("category_main", "Thời Trang Test");
        row.put("display_order", 1);

        List<Map<String, Object>> rows = List.of(row);

        // Execute Confirm Categories Import
        importService.confirmCategoriesImport(categoryJob.getId(), rows, "admin");
        ImportJob finishedJob = waitForJob(categoryJob.getId());

        assertNotNull(finishedJob);
        assertEquals("COMPLETED", finishedJob.getStatus());
        assertEquals(1, finishedJob.getSuccessRows());
        assertEquals(0, finishedJob.getFailedRows());

        // Verify CategoryMain is created in Database
        Optional<CategoryMain> mainOpt = categoryMainRepository.findByNameAndDeletedAtIsNull("Thời Trang Test");
        assertTrue(mainOpt.isPresent());
        assertEquals("Thời Trang Test", mainOpt.get().getName());
        assertNotNull(mainOpt.get().getSlug());

        // Verify Category created in Database has CategoryMain linked
        Optional<Category> catOpt = categoryRepository.findBySlugAndDeletedAtIsNull("new-child-category-with-main");
        assertTrue(catOpt.isPresent());
        assertNotNull(catOpt.get().getCategoryMain());
        assertEquals(mainOpt.get().getId(), catOpt.get().getCategoryMain().getId());

        // Clean up
        categoryRepository.delete(catOpt.get());
        categoryMainRepository.delete(mainOpt.get());
    }

    private ImportJob waitForJob(Long jobId) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 10000) { // 10 seconds timeout
            ImportJob job = jobRepository.findById(jobId).orElse(null);
            if (job != null && !List.of("PENDING", "PROCESSING").contains(job.getStatus())) {
                return job;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return jobRepository.findById(jobId).orElse(null);
    }
}
