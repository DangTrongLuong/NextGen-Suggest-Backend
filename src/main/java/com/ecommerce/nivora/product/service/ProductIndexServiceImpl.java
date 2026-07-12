package com.ecommerce.nivora.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexServiceImpl implements ProductIndexService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    @Override
    @Async
    public void indexProductAsync(String productUuid) {
        if (productUuid == null || productUuid.isBlank()) {
            return;
        }
        try {
            log.info("Sending async index message to RabbitMQ for product UUID: {}", productUuid);
            Map<String, Object> message = Map.of(
                "action", "sync",
                "product_uuids", List.of(productUuid)
            );
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("Successfully published sync message for product UUID: {}", productUuid);
        } catch (Exception e) {
            log.error("Failed to publish sync message for product UUID {} to RabbitMQ", productUuid, e);
        }
    }

    @Override
    @Async
    public void deleteProductAsync(String productUuid) {
        if (productUuid == null || productUuid.isBlank()) {
            return;
        }
        try {
            log.info("Sending async delete message to RabbitMQ for product UUID: {}", productUuid);
            Map<String, Object> message = Map.of(
                "action", "delete",
                "product_uuids", List.of(productUuid)
            );
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("Successfully published delete message for product UUID: {}", productUuid);
        } catch (Exception e) {
            log.error("Failed to publish delete message for product UUID {} to RabbitMQ", productUuid, e);
        }
    }
}
