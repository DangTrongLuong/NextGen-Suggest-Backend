package com.ecommerce.nivora.recommendation.dto.response;

import com.ecommerce.nivora.recommendation.FastApiProductResult;


import lombok.Data;
import java.util.List;

@Data
public class FastApiRecommendResponse {
    private List<FastApiProductResult> recommendations;
    private int total;
    private String strategy;
    private double latencyMs;
}
