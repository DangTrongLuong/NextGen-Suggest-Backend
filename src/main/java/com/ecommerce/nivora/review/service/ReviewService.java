package com.ecommerce.nivora.review.service;

import com.ecommerce.nivora.common.dto.response.PageResponse;
import com.ecommerce.nivora.review.dto.request.ReviewRequest;
import com.ecommerce.nivora.review.dto.response.ReviewResponse;


import org.springframework.data.domain.Pageable;

public interface ReviewService {
    PageResponse<ReviewResponse> getByProduct(Long productId, Pageable pageable);
    ReviewResponse create(Long userId, ReviewRequest request);
    void delete(Long userId, Long reviewId);
}
