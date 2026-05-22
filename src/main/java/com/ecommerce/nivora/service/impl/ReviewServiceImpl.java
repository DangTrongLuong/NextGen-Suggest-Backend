package com.ecommerce.nivora.service.impl;

import com.ecommerce.nivora.common.PageResponse;
import com.ecommerce.nivora.dto.request.ReviewRequest;
import com.ecommerce.nivora.dto.response.ReviewResponse;
import com.ecommerce.nivora.entity.Product;
import com.ecommerce.nivora.entity.Review;
import com.ecommerce.nivora.entity.User;
import com.ecommerce.nivora.exception.BadRequestException;
import com.ecommerce.nivora.exception.ResourceNotFoundException;
import com.ecommerce.nivora.repository.ProductRepository;
import com.ecommerce.nivora.repository.ReviewRepository;
import com.ecommerce.nivora.repository.UserRepository;
import com.ecommerce.nivora.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getByProduct(Long productId, Pageable pageable) {
        return PageResponse.of(
            reviewRepository.findByProductIdAndIsApprovedTrueAndDeletedAtIsNullOrderByCreatedAtDesc(productId, pageable)
                .map(this::toResponse));
    }

    @Override
    @Transactional
    public ReviewResponse create(Long userId, ReviewRequest request) {
        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), userId)) {
            throw new BadRequestException("You have already reviewed this product");
        }

        Product product = productRepository.findByIdAndDeletedAtIsNull(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .body(request.getBody())
                .isApproved(true)
                .build();

        reviewRepository.save(review);
        updateProductRating(product);
        return toResponse(review);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own reviews");
        }

        review.setDeletedAt(LocalDateTime.now());
        reviewRepository.save(review);
        updateProductRating(review.getProduct());
    }

    private void updateProductRating(Product product) {
        Double avg = reviewRepository.getAverageRating(product.getId());
        long count = reviewRepository.countApprovedByProductId(product.getId());
        product.setRatingAvg(avg != null
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        product.setRatingCount((int) count);
        productRepository.save(product);
    }

    private ReviewResponse toResponse(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .productId(r.getProduct().getId())
                .authorName(r.getUser().getName())
                .authorAvatar(r.getUser().getAvatarUrl())
                .rating(r.getRating())
                .title(r.getTitle())
                .body(r.getBody())
                .isVerified(r.getIsVerified())
                .helpfulCount(r.getHelpfulCount())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
