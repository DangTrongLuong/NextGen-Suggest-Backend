package com.ecommerce.nivora.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisConnectionTest {

    @Autowired
    private RedisService redisService;

    @Test
    void testRedisConnection() {
        String testKey = "test:connection:key";
        String testValue = "connection_ok";

        // Set value
        redisService.set(testKey, testValue, 1, TimeUnit.MINUTES);

        // Get value
        Object fetchedValue = redisService.get(testKey);

        // Assert
        assertThat(fetchedValue).isEqualTo(testValue);

        // Delete key
        redisService.delete(testKey);

        // Confirm deleted
        assertThat(redisService.hasKey(testKey)).isFalse();

        System.out.println("=================================================");
        System.out.println(">>> REDIS CONNECTION TEST PASSED SUCCESSFULLY! <<<");
        System.out.println("=================================================");
    }
}
