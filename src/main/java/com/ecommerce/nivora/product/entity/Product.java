package com.ecommerce.nivora.product.entity;

import com.ecommerce.nivora.category.entity.Category;
import com.ecommerce.nivora.review.entity.Review;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_slug", columnList = "slug", unique = true),
    @Index(name = "idx_product_category_id", columnList = "category_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    @Builder.Default
    private String uuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 300)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String brand;

    @Column(name = "product_url", length = 500)
    private String productUrl;

    @Formula("(SELECT COALESCE(MIN(v.price), 0) FROM product_variants v WHERE v.product_id = id AND v.is_active = true AND v.deleted_at IS NULL)")
    private BigDecimal basePrice;

    @Formula("(SELECT v.compare_price FROM product_variants v WHERE v.product_id = id AND v.is_active = true AND v.deleted_at IS NULL ORDER BY v.price ASC LIMIT 1)")
    private BigDecimal comparePrice;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(precision = 8, scale = 3)
    private BigDecimal weight;

    /** Ảnh đại diện chính của sản phẩm (ngoài bảng product_images). */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /** JSON attributes mặc định, ví dụ {"material":"cotton"} */
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String attributes = "{}";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "is_new", nullable = false)
    @Builder.Default
    private Boolean isNew = false;

    @Column(name = "is_best_seller", nullable = false)
    @Builder.Default
    private Boolean isBestSeller = false;

    @Column(name = "views", nullable = false, columnDefinition = "int default 0")
    @Builder.Default
    private Integer views = 0;

    @Column(name = "sales_count", nullable = false, columnDefinition = "bigint default 0")
    @Builder.Default
    private Long salesCount = 0L;

    @Formula("(SELECT COALESCE(SUM(i.quantity), 0) FROM product_variants v JOIN inventories i ON i.variant_id = v.id WHERE v.product_id = id AND v.is_active = true AND v.deleted_at IS NULL)")
    private Integer stock;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(name = "seo_title", length = 255)
    private String seoTitle;

    @Column(name = "seo_description", columnDefinition = "TEXT")
    private String seoDescription;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (uuid == null)
            uuid = UUID.randomUUID().toString();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getSku() {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream()
                .filter(ProductVariant::getIsActive)
                .map(ProductVariant::getSku)
                .findFirst()
                .orElse(null);
    }

    public String getColor() {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream()
                .filter(ProductVariant::getIsActive)
                .map(ProductVariant::getColor)
                .filter(Objects::nonNull)
                .filter(c -> !c.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
    }

    public String getSize() {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream()
                .filter(ProductVariant::getIsActive)
                .map(ProductVariant::getSize)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(","));
    }
}