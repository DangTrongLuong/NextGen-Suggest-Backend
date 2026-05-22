package com.ecommerce.nivora.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariantResponse {
    private Long id;
    private String sku;
    private String name;
    private BigDecimal price;
    private BigDecimal comparePrice;
    private String attributes;
    private String imageUrl;
    private Boolean isActive;
    private Integer sortOrder;
    private Integer stock;
    private Integer availableStock;
}
