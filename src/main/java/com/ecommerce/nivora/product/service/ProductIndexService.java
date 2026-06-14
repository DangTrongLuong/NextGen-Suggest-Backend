package com.ecommerce.nivora.product.service;

public interface ProductIndexService {
    void indexProductAsync(String productUuid);
    void deleteProductAsync(String productUuid);
}
