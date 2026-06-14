package com.ecommerce.nivora.admin.service;

import com.ecommerce.nivora.admin.entity.ImportJob;
import com.ecommerce.nivora.admin.repository.ImportJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressTrackingServiceImpl implements ProgressTrackingService {

    private final ImportJobRepository jobRepository;
    private final ObjectMapper objectMapper;
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter registerEmitter(Long jobId) {
        SseEmitter emitter = new SseEmitter(1800000L); // 30 mins timeout
        emitters.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        emitter.onCompletion(() -> removeEmitter(jobId, emitter));
        emitter.onTimeout(() -> removeEmitter(jobId, emitter));
        emitter.onError(e -> removeEmitter(jobId, emitter));
        
        log.info("Registered SSE emitter for Import Job: {}", jobId);
        
        // Send initial connection check
        try {
            emitter.send(SseEmitter.event().name("init").data(Map.of("message", "Connected to progress stream")));
        } catch (IOException e) {
            log.error("Failed to send init SSE event for Job: {}", jobId, e);
            removeEmitter(jobId, emitter);
        }
        
        return emitter;
    }

    private void removeEmitter(Long jobId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(jobId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(jobId);
            }
        }
        log.info("Removed SSE emitter for Import Job: {}", jobId);
    }

    @Override
    public void updateProgress(Long jobId, int successCount, int duplicateCount, int failedCount, String status,
                               List<Map<String, Object>> duplicateDetails, List<Map<String, Object>> errorDetails) {
        ImportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        job.setSuccessRows(successCount);
        job.setDuplicateRows(duplicateCount);
        job.setFailedRows(failedCount);
        job.setStatus(status);
        
        try {
            if (duplicateDetails != null && !duplicateDetails.isEmpty()) {
                job.setDuplicateDetails(objectMapper.writeValueAsString(duplicateDetails));
            }
            if (errorDetails != null && !errorDetails.isEmpty()) {
                job.setErrorDetails(objectMapper.writeValueAsString(errorDetails));
            }
        } catch (Exception e) {
            log.error("Failed to serialize details for Job: {}", jobId, e);
        }

        jobRepository.save(job);
        broadcastProgress(jobId, job);
    }

    @Override
    public void completeJob(Long jobId, int successCount, int duplicateCount, int failedCount,
                            List<Map<String, Object>> duplicateDetails, List<Map<String, Object>> errorDetails) {
        ImportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        job.setSuccessRows(successCount);
        job.setDuplicateRows(duplicateCount);
        job.setFailedRows(failedCount);
        job.setStatus(failedCount == 0 ? "COMPLETED" : (successCount > 0 ? "COMPLETED" : "FAILED"));
        job.setFinishedAt(LocalDateTime.now());
        
        try {
            if (duplicateDetails != null && !duplicateDetails.isEmpty()) {
                job.setDuplicateDetails(objectMapper.writeValueAsString(duplicateDetails));
            }
            if (errorDetails != null && !errorDetails.isEmpty()) {
                job.setErrorDetails(objectMapper.writeValueAsString(errorDetails));
            }
        } catch (Exception e) {
            log.error("Failed to serialize details for Job: {}", jobId, e);
        }

        jobRepository.save(job);
        broadcastProgress(jobId, job);
        closeEmitters(jobId);
    }

    @Override
    public void failJob(Long jobId, String errorMsg) {
        ImportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        job.setStatus("FAILED");
        job.setFinishedAt(LocalDateTime.now());
        
        List<Map<String, Object>> errorDetails = new ArrayList<>();
        errorDetails.add(Map.of("row", 0, "reason", errorMsg));
        try {
            job.setErrorDetails(objectMapper.writeValueAsString(errorDetails));
        } catch (Exception e) {
            log.error("Failed to serialize error for Job: {}", jobId, e);
        }

        jobRepository.save(job);
        broadcastProgress(jobId, job);
        closeEmitters(jobId);
    }

    @Override
    public ImportJob getJobStatus(Long jobId) {
        return jobRepository.findById(jobId).orElse(null);
    }

    private void broadcastProgress(Long jobId, ImportJob job) {
        List<SseEmitter> list = emitters.get(jobId);
        if (list == null || list.isEmpty()) {
            return;
        }

        int total = job.getTotalRows() != null ? job.getTotalRows() : 0;
        int success = job.getSuccessRows() != null ? job.getSuccessRows() : 0;
        int duplicate = job.getDuplicateRows() != null ? job.getDuplicateRows() : 0;
        int failed = job.getFailedRows() != null ? job.getFailedRows() : 0;
        int processed = success + duplicate + failed;
        double percentage = total > 0 ? ((double) processed / total) * 100.0 : 0.0;
        
        // Rounded to 1 decimal place
        percentage = Math.round(percentage * 10.0) / 10.0;

        Map<String, Object> progressPayload = new HashMap<>();
        progressPayload.put("jobId", jobId);
        progressPayload.put("status", job.getStatus());
        progressPayload.put("totalRows", total);
        progressPayload.put("successRows", success);
        progressPayload.put("duplicateRows", duplicate);
        progressPayload.put("failedRows", failed);
        progressPayload.put("processedRows", processed);
        progressPayload.put("percentage", percentage);
        
        try {
            if (job.getDuplicateDetails() != null) {
                progressPayload.put("duplicateDetails", objectMapper.readValue(job.getDuplicateDetails(), List.class));
            } else {
                progressPayload.put("duplicateDetails", List.of());
            }
            if (job.getErrorDetails() != null) {
                progressPayload.put("errorDetails", objectMapper.readValue(job.getErrorDetails(), List.class));
            } else {
                progressPayload.put("errorDetails", List.of());
            }
        } catch (Exception e) {
            log.error("Failed to deserialize details for broadcast", e);
        }

        log.info("Broadcasting progress for job {}: processed {}/{}", jobId, processed, total);

        List<SseEmitter> failedEmitters = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("progress").data(progressPayload));
            } catch (Exception e) {
                log.warn("Failed to send SSE to emitter, removing. Error: {}", e.getMessage());
                failedEmitters.add(emitter);
            }
        }
        
        failedEmitters.forEach(emitter -> removeEmitter(jobId, emitter));
    }

    private void closeEmitters(Long jobId) {
        List<SseEmitter> list = emitters.get(jobId);
        if (list != null) {
            for (SseEmitter emitter : list) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {}
            }
            emitters.remove(jobId);
        }
    }
}
