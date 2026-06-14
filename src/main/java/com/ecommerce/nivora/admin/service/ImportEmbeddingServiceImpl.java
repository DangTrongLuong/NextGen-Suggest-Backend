package com.ecommerce.nivora.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ImportEmbeddingServiceImpl implements ImportEmbeddingService {
    @Override
    public void logEmbeddingCreation(String uuid, String itemName) {
        log.info("Generating semantic embeddings for product: {} (UUID: {})", itemName, uuid);
    }
}
