package com.ecommerce.nivora.service;

import com.ecommerce.nivora.common.PageResponse;
import com.ecommerce.nivora.dto.request.ProductRequest;
import com.ecommerce.nivora.dto.response.ProductResponse;
import com.ecommerce.nivora.dto.response.ProductVariantResponse;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ProductService {
    PageResponse<ProductResponse> getAll(Long categoryId, String categorySlug, String brand, BigDecimal minPrice,
                                         BigDecimal maxPrice, BigDecimal minRating,
                                         String search, Pageable pageable);
    PageResponse<ProductResponse> getAllForAdmin(Long categoryId, String search, Pageable pageable);
    ProductResponse getById(Long id);
    ProductResponse getByIdForAdmin(Long id);
    ProductResponse getBySlug(String slug);
    List<ProductVariantResponse> getVariants(Long productId);
    List<ProductResponse> getFeatured();
    List<ProductResponse> getBestSellers();
    List<ProductResponse> getNewArrivals();
    List<ProductResponse> getRelated(Long productId, int limit);
    Map<String, ProductResponse> getActiveProductsBySkus(Collection<String> skus);
    ProductResponse create(ProductRequest request);
    ProductResponse update(Long id, ProductRequest request);
    void delete(Long id);
}
