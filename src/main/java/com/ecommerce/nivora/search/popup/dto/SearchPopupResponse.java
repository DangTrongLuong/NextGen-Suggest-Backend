package com.ecommerce.nivora.search.popup.dto;

import com.ecommerce.nivora.product.dto.response.ProductResponse;
import com.ecommerce.nivora.category.dto.response.CategoryResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchPopupResponse {
    private List<String> relatedSearches;
    private List<String> searchHistory;
    private List<ProductResponse> recentlyViewed;
    private List<String> trendingSearches;
    private List<CategoryResponse> popularCategories;
}
