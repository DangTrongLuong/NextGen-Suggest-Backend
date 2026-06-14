package com.ecommerce.nivora.admin.service;

import com.ecommerce.nivora.admin.entity.ImportJob;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.Map;

public interface ProgressTrackingService {
    SseEmitter registerEmitter(Long jobId);
    void updateProgress(Long jobId, int successCount, int duplicateCount, int failedCount, String status, List<Map<String, Object>> duplicateDetails, List<Map<String, Object>> errorDetails);
    void completeJob(Long jobId, int successCount, int duplicateCount, int failedCount, List<Map<String, Object>> duplicateDetails, List<Map<String, Object>> errorDetails);
    void failJob(Long jobId, String errorMsg);
    ImportJob getJobStatus(Long jobId);
}
