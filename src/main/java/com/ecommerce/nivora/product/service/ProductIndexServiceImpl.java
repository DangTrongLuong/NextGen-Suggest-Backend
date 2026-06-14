package com.ecommerce.nivora.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexServiceImpl implements ProductIndexService {

    private final RestTemplate restTemplate;

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @Override
    @Async
    public void indexProductAsync(String productUuid) {
        if (productUuid == null || productUuid.isBlank()) {
            return;
        }
        try {
            String url = fastApiUrl + "/api/v1/indexing/sync";
            log.info("Sending async index request to FastAPI for product UUID: {}", productUuid);
            Map<String, Object> request = Map.of("product_uuids", List.of(productUuid));
            restTemplate.postForEntity(url, request, Void.class);
            log.info("Successfully sent async index request for product UUID: {}", productUuid);
        } catch (Exception e) {
            log.error("Failed to index product UUID {} in Qdrant", productUuid, e);
        }
    }

    @Override
    @Async
    public void deleteProductAsync(String productUuid) {
        if (productUuid == null || productUuid.isBlank()) {
            return;
        }
        try {
            String url = fastApiUrl + "/api/v1/indexing/delete";
            log.info("Sending async delete request to FastAPI for product UUID: {}", productUuid);
            Map<String, Object> request = Map.of("product_uuids", List.of(productUuid));
            restTemplate.postForEntity(url, request, Void.class);
            log.info("Successfully sent async delete request for product UUID: {}", productUuid);
        } catch (Exception e) {
            log.error("Failed to delete product UUID {} from Qdrant", productUuid, e);
        }
    }
}
