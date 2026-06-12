package com.ecommerce.nivora.search.dto.response;

import com.ecommerce.nivora.recommendation.FastApiProductResult;


import lombok.Data;
import java.util.List;

@Data
public class FastApiSearchResponse {
    private String query;
    private List<FastApiProductResult> results;
    private Integer total;
}
