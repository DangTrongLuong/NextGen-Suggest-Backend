package com.ecommerce.nivora.analytics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics Proxy", description = "Proxy click tracking events to FastAPI analytics engine")
public class AnalyticsProxyController {

    private final RestTemplate restTemplate;

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @PostMapping("/search/click")
    @Operation(summary = "Track search click event via Spring Boot gateway")
    public ResponseEntity<Map> trackClick(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        try {
            if (sessionId != null && (!body.containsKey("session_id") || body.get("session_id") == null)) {
                body.put("session_id", sessionId);
            }

            String url = fastApiUrl + "/analytics/search/click";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            if (sessionId != null) {
                headers.set("X-Session-Id", sessionId);
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            log.error("Failed to proxy click tracking event to FastAPI", e);
            return ResponseEntity.ok(Map.of("success", true, "message", "Handled with fallback"));
        }
    }
}
