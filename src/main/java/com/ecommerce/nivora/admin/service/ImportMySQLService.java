package com.ecommerce.nivora.admin.service;

import com.ecommerce.nivora.category.entity.Category;
import com.ecommerce.nivora.product.entity.Product;
import java.util.Map;

public interface ImportMySQLService {
    boolean isProductDuplicate(Map<String, Object> row);
    boolean isCategoryDuplicate(Map<String, Object> row);
    Product saveProductRow(Map<String, Object> row);
    Category saveCategoryRow(Map<String, Object> row);
    void deleteProductByUuid(String uuid);
}
