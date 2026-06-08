package com.ecommerce.nivora.service;

import com.ecommerce.nivora.dto.response.DashboardResponse;
import java.util.List;
import java.util.Map;

public interface DashboardService {
    DashboardResponse getStats();
    List<Map<String, Object>> getRevenueChart();
    List<Map<String, Object>> getTopProducts();
    List<Map<String, Object>> getCategoryStats();
}
