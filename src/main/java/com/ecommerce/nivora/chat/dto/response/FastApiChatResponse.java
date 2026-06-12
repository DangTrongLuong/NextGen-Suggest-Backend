package com.ecommerce.nivora.chat.dto.response;

import com.ecommerce.nivora.recommendation.FastApiProductResult;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class FastApiChatResponse {
    private String response;
    
    @JsonProperty("suggested_products")
    private List<FastApiProductResult> suggestedProducts;
}
