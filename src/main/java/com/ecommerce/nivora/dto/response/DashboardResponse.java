package com.ecommerce.nivora.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardResponse {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalProducts;
    private long totalCustomers;
    private long pendingOrders;
    private long processingOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long lowStockProducts;
}
