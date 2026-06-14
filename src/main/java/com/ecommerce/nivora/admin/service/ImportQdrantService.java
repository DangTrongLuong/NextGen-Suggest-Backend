package com.ecommerce.nivora.admin.service;

public interface ImportQdrantService {
    void syncProduct(String uuid) throws Exception;
    void syncProducts(java.util.List<String> uuids) throws Exception;
}
