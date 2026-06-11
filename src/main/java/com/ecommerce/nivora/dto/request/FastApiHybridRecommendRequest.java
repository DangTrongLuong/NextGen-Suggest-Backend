package com.ecommerce.nivora.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastApiHybridRecommendRequest {
    private List<String> viewed_product_ids;
    private List<String> wishlist_product_ids;
    private List<String> purchased_product_ids;
    private List<String> popular_product_ids;
    private Integer top_k;
    private Boolean exclude_viewed;
}
