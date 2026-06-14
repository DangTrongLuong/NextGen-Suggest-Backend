package com.ecommerce.nivora.admin.service;

import com.ecommerce.nivora.admin.entity.ImportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface ImportService {
    Map<String, Object> previewProducts(MultipartFile file, String username);
    Map<String, Object> previewCategories(MultipartFile file, String username);
    Map<String, Object> confirmProductsImport(Long jobId, List<Map<String, Object>> rows, String username);
    Map<String, Object> confirmCategoriesImport(Long jobId, List<Map<String, Object>> rows, String username);
    Page<ImportJob> getImportHistory(Pageable pageable);
}
