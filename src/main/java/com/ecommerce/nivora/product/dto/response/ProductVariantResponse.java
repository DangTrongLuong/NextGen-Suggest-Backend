package com.ecommerce.nivora.product.dto.response;


import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {
    private Long id;
    private String sku;
    private String name;
    private BigDecimal price;
    private BigDecimal comparePrice;

    /** Màu sắc variant, ví dụ "Đỏ", "Xanh dương" */
    private String color;

    /** Size variant, ví dụ "S", "M", "XL", "42" */
    private String size;

    private String attributes;
    private String imageUrl;
    private Boolean isActive;
    private Integer sortOrder;
    private Integer stock;
    private Integer availableStock;
}