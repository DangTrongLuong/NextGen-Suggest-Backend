package com.ecommerce.nivora.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private Long variantId;
    private String productName;
    private String variantName;
    private String sku;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
