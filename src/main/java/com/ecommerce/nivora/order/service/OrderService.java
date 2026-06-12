package com.ecommerce.nivora.order.service;

import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.order.dto.request.CheckoutRequest;
import com.ecommerce.nivora.order.dto.response.OrderResponse;


import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse checkout(Long userId, CheckoutRequest request);
    PageResponse<OrderResponse> getUserOrders(Long userId, Pageable pageable);
    OrderResponse getOrderById(Long userId, Long orderId);
    OrderResponse getOrderByNumber(String orderNumber);
    OrderResponse getOrderByIdAdmin(Long orderId);
    OrderResponse updateStatus(Long orderId, String status);
    OrderResponse cancelOrder(Long userId, Long orderId, String reason);
    PageResponse<OrderResponse> getAllOrders(Pageable pageable);
    PageResponse<OrderResponse> getAllOrders(String status, Pageable pageable);
}
