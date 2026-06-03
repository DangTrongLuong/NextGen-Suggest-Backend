package com.ecommerce.nivora.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class FastApiSearchResponse {
    private String query;
    private List<FastApiProductResult> results;
    private Integer total;
}
