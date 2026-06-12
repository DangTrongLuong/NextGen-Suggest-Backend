package com.ecommerce.nivora.recommendation;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FastApiProductResult {
    @JsonProperty("product_id")
    private String productId;
    
    private String title;
    private String description;
    private String category;
    private Double price;
    
    @JsonProperty("image_url")
    private String imageUrl;
    
    private Double score;
    
    @JsonProperty("reranker_score")
    private Double rerankerScore;
    
    private Integer rank;
    private String reason;
}
