package com.ecommerce.nivora.category.dto.response;


import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private String icon;
    private Integer sortOrder;
    private Boolean isActive;
    private Long parentId;
    private String parentName;
    private List<CategoryResponse> children;
    private Integer productCount;
}
