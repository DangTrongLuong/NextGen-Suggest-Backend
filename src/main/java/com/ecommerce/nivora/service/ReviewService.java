package com.ecommerce.nivora.service;

import com.ecommerce.nivora.common.PageResponse;
import com.ecommerce.nivora.dto.request.ReviewRequest;
import com.ecommerce.nivora.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    PageResponse<ReviewResponse> getByProduct(Long productId, Pageable pageable);
    ReviewResponse create(Long userId, ReviewRequest request);
    void delete(Long userId, Long reviewId);
}
