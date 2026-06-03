package com.ecommerce.nivora.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String uuid;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private String brand;
    private String sku;
    private Integer stock;
    private BigDecimal basePrice;
    private BigDecimal comparePrice;
    private BigDecimal taxRate;

    // ── Variant-aligned fields ───────────────────────────────────────────────
    private String color;
    private String size;
    private String imageUrl;
    private String attributes;
    // ────────────────────────────────────────────────────────────────────────

    private Boolean isActive;
    private Boolean isFeatured;
    private Boolean isNew;
    private Boolean isBestSeller;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private CategoryResponse category;
    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}