package com.ecommerce.nivora.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class FastApiRecommendResponse {
    private List<FastApiProductResult> recommendations;
    private int total;
    private String strategy;
    private double latencyMs;
}
