package com.ecommerce.nivora.search.dto.response;

import com.ecommerce.nivora.recommendation.FastApiProductResult;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class FastApiSearchResponse {
    @JsonProperty("request_id")
    private String requestId;
    private String query;
    private List<FastApiProductResult> results;
    private Integer total;
}
