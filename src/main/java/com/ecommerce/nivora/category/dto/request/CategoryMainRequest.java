package com.ecommerce.nivora.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryMainRequest {

    @NotBlank(message = "CategoryMain name is required")
    @Size(min = 2, max = 150)
    private String name;

    private String slug;
    
    private String imageUrl;

    @Builder.Default
    private Integer displayOrder = 0;
}
