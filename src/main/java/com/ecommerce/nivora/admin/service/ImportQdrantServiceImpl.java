package com.ecommerce.nivora.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportQdrantServiceImpl implements ImportQdrantService {

    private final RestTemplate restTemplate;

    @Value("${app.fastapi-url}")
    private String fastApiUrl;

    @Override
    public void syncProduct(String uuid) throws Exception {
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("Product UUID cannot be empty");
        }
        String url = fastApiUrl + "/api/v1/indexing/sync";
        log.info("Sending synchronous indexing request to FastAPI for product UUID: {}", uuid);
        Map<String, Object> request = Map.of("product_uuids", List.of(uuid));
        restTemplate.postForEntity(url, request, Void.class);
        log.info("Successfully indexed product in Qdrant. UUID: {}", uuid);
    }

    @Override
    public void syncProducts(List<String> uuids) throws Exception {
        if (uuids == null || uuids.isEmpty()) {
            return;
        }
        String url = fastApiUrl + "/api/v1/indexing/sync";
        log.info("Sending batch indexing request to FastAPI for {} product UUIDs", uuids.size());
        Map<String, Object> request = Map.of("product_uuids", uuids);
        restTemplate.postForEntity(url, request, Void.class);
        log.info("Successfully indexed {} products in Qdrant.", uuids.size());
    }
}
