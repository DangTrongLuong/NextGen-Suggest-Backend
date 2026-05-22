package com.ecommerce.nivora.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewResponse {
    private Long id;
    private Long productId;
    private String authorName;
    private String authorAvatar;
    private Integer rating;
    private String title;
    private String body;
    private Boolean isVerified;
    private Integer helpfulCount;
    private LocalDateTime createdAt;
}
