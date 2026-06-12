package com.ecommerce.nivora.recommendation.dto.response;

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
public class HomepageRecommendationResponse {
    private List<ProductResponse> personal;      // Dành riêng cho bạn (Hybrid Rec)
    private List<ProductResponse> trending;      // Xu hướng hôm nay (Trending Today)
    private List<ProductResponse> recent;        // Tiếp tục xem (Continue Browsing)
    private List<ProductResponse> youMayNeed;    // Có thể bạn thích (Cross-sell)
    private List<ProductResponse> bestSellers;   // Bán chạy nhất
    private List<ProductResponse> newArrivals;   // Hàng mới về
    private List<ProductResponse> featured;      // Lựa chọn nổi bật
}
