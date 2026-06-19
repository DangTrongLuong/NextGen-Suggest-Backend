package com.ecommerce.nivora.category.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryMainResponse {
    private Long id;
    private String name;
    private String slug;
    private String imageUrl;
    private Integer displayOrder;
    private LocalDateTime createdAt;
}
