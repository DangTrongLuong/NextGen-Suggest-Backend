package com.ecommerce.nivora.search.popup.dto;

import com.ecommerce.nivora.category.dto.response.CategoryResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutocompleteResponse {
    private String query;
    private List<Suggestion> suggestions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Suggestion {
        private String keyword;
        private String type; // HISTORY, TRENDING, AI_SUGGESTION
        private CategoryResponse category; // Optional category mapping
    }
}
