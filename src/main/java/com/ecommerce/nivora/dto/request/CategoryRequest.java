package com.ecommerce.nivora.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 150)
    private String name;

    private String slug;
    private String description;
    private String imageUrl;
    private String icon;
    private Long parentId;

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private Boolean isActive = true;

    private String seoTitle;
    private String seoDescription;
}
