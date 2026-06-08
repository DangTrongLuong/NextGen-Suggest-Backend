package com.ecommerce.nivora.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    private String name;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @Size(max = 500)
    private String shortDescription;

    private String description;

    @Size(max = 100)
    private String brand;

    @Size(max = 100)
    private String sku;

    @Min(value = 0, message = "Stock must be non-negative")
    private Integer stock;

    @DecimalMin(value = "0.0", message = "Price must be positive")
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0", message = "Compare price must be positive")
    private BigDecimal comparePrice;

    @DecimalMin(value = "0.0", message = "Cost price must be positive")
    private BigDecimal costPrice;

    @DecimalMin(value = "0.0", message = "Tax rate must be positive")
    @DecimalMax(value = "100.0", message = "Tax rate must be <= 100")
    private BigDecimal taxRate;

    @DecimalMin(value = "0.0", message = "Weight must be positive")
    private BigDecimal weight;

    // ── Variant-aligned fields ───────────────────────────────────────────────
    /** Màu sắc mặc định của sản phẩm gốc */
    @Size(max = 50)
    private String color;

    /** Size mặc định của sản phẩm gốc */
    @Size(max = 50)
    private String size;

    /** Ảnh đại diện chính (không qua bảng product_images) */
    private String imageUrl;

    /** JSON attributes mặc định */
    @Size(max = 2000)
    private String attributes;
    // ────────────────────────────────────────────────────────────────────────

    @Builder.Default
    private Boolean isActive = true;
    @Builder.Default
    private Boolean isFeatured = false;
    @Builder.Default
    private Boolean isNew = false;
    @Builder.Default
    private Boolean isBestSeller = false;

    @Size(max = 255)
    private String seoTitle;
    private String seoDescription;

    private List<@NotBlank String> imageUrls;

    @Valid
    private List<VariantRequest> variants;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VariantRequest {
        @NotBlank
        @Size(max = 120)
        private String sku;
        @NotBlank
        @Size(max = 255)
        private String name;
        @NotNull
        @DecimalMin("0.0")
        private BigDecimal price;
        @DecimalMin("0.0")
        private BigDecimal comparePrice;

        /** Màu sắc của variant, ví dụ "Đỏ", "#FF0000" */
        @Size(max = 50)
        private String color;

        /** Size của variant, ví dụ "S", "M", "XL" */
        @Size(max = 50)
        private String size;

        /** JSON attributes bổ sung, ví dụ {"material":"cotton"} */
        @Size(max = 2000)
        private String attributes;

        private String imageUrl;
        @NotNull
        @Min(0)
        private Integer stock;
        @Builder.Default
        private Boolean isActive = true;
        @Builder.Default
        private Integer sortOrder = 0;
    }
}