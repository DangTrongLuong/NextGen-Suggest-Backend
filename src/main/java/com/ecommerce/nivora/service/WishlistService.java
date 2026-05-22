package com.ecommerce.nivora.service;

import com.ecommerce.nivora.dto.response.ProductResponse;
import java.util.List;

public interface WishlistService {
    List<ProductResponse> getWishlist(Long userId);
    void addToWishlist(Long userId, Long productId);
    void removeFromWishlist(Long userId, Long productId);
    boolean isInWishlist(Long userId, Long productId);
}
