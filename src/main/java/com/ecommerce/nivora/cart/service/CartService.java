package com.ecommerce.nivora.cart.service;

import com.ecommerce.nivora.cart.dto.request.CartItemRequest;
import com.ecommerce.nivora.cart.dto.response.CartResponse;



public interface CartService {
    CartResponse getCart(Long userId);
    CartResponse addItem(Long userId, CartItemRequest request);
    CartResponse updateItem(Long userId, Long variantId, Integer quantity);
    CartResponse removeItem(Long userId, Long variantId);
    void clearCart(Long userId);
}
