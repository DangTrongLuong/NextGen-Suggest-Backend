package com.ecommerce.nivora.service.impl;

import com.ecommerce.nivora.dto.response.DashboardResponse;
import com.ecommerce.nivora.entity.Order;
import com.ecommerce.nivora.repository.*;
import com.ecommerce.nivora.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getStats() {
        return DashboardResponse.builder()
                .totalRevenue(orderRepository.getTotalRevenue())
                .totalOrders(orderRepository.count())
                .totalProducts(productRepository.countByDeletedAtIsNull())
                .totalCustomers(userRepository.count())
                .pendingOrders(orderRepository.countByStatus(Order.Status.PENDING))
                .processingOrders(orderRepository.countByStatus(Order.Status.CONFIRMED)
                        + orderRepository.countByStatus(Order.Status.PACKING))
                .deliveredOrders(orderRepository.countByStatus(Order.Status.DELIVERED))
                .cancelledOrders(orderRepository.countByStatus(Order.Status.CANCELLED))
                .lowStockProducts(inventoryRepository.findLowStockItems().size())
                .build();
    }
}
