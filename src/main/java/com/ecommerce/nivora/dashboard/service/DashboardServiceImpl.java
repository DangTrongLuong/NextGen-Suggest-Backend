package com.ecommerce.nivora.dashboard.service;

import com.ecommerce.nivora.dashboard.dto.response.DashboardResponse;
import com.ecommerce.nivora.order.entity.Order;
import com.ecommerce.nivora.order.repository.OrderRepository;
import com.ecommerce.nivora.product.repository.InventoryRepository;
import com.ecommerce.nivora.product.repository.ProductRepository;
import com.ecommerce.nivora.user.repository.UserRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRevenueChart() {
        java.time.LocalDateTime startDate = java.time.LocalDateTime.now().minusDays(30);
        List<Object[]> summaries = orderRepository.getOrderSummaryForCharts(startDate);
        
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.util.Map<String, java.math.BigDecimal> revenueMap = new java.util.TreeMap<>();
        java.util.Map<String, Long> orderCountMap = new java.util.TreeMap<>();
        
        // Pre-fill last 30 days
        for (int i = 29; i >= 0; i--) {
            String dateKey = java.time.LocalDate.now().minusDays(i).format(formatter);
            revenueMap.put(dateKey, java.math.BigDecimal.ZERO);
            orderCountMap.put(dateKey, 0L);
        }
        
        for (Object[] row : summaries) {
            java.time.LocalDateTime createdAt = (java.time.LocalDateTime) row[0];
            java.math.BigDecimal totalAmount = (java.math.BigDecimal) row[1];
            Order.Status status = (Order.Status) row[2];
            
            String dateKey = createdAt.toLocalDate().format(formatter);
            if (revenueMap.containsKey(dateKey)) {
                if (status == Order.Status.DELIVERED) {
                    revenueMap.put(dateKey, revenueMap.get(dateKey).add(totalAmount));
                }
                orderCountMap.put(dateKey, orderCountMap.get(dateKey) + 1);
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (String dateKey : revenueMap.keySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", dateKey);
            point.put("revenue", revenueMap.get(dateKey));
            point.put("orders", orderCountMap.get(dateKey));
            result.add(point);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopProducts() {
        List<Object[]> rows = orderRepository.getTopSellingProducts(org.springframework.data.domain.PageRequest.of(0, 5));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", row[0]);
            map.put("soldCount", row[1]);
            map.put("revenue", row[2]);
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCategoryStats() {
        List<Object[]> rows = productRepository.getProductCountByCategory();
        List<Map<String, Object>> result = new ArrayList<>();
        long total = rows.stream().mapToLong(row -> (Long) row[1]).sum();
        
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            String name = (String) row[0];
            long count = (Long) row[1];
            double percentage = total > 0 ? (double) count / total * 100 : 0;
            
            map.put("name", name != null ? name : "Chưa phân loại");
            map.put("count", count);
            map.put("percentage", Math.round(percentage * 10.0) / 10.0);
            result.add(map);
        }
        return result;
    }
}
