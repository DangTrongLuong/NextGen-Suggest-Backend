package com.ecommerce.nivora.chat.dto.response;

import com.ecommerce.nivora.product.dto.response.ProductResponse;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String response;
    private List<ProductResponse> suggestedProducts;
}
