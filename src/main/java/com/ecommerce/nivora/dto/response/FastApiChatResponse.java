package com.ecommerce.nivora.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class FastApiChatResponse {
    private String response;
    
    @JsonProperty("suggested_products")
    private List<FastApiProductResult> suggestedProducts;
}
