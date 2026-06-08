package com.ecommerce.nivora.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImageResponse {
    private Long id;
    private String url;
    private String altText;
    private Integer sortOrder;
    private Boolean isPrimary;
    private Long variantId;
}
