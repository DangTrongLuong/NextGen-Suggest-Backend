package com.ecommerce.nivora.search.popup.controller;

import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.search.popup.dto.AutocompleteResponse;
import com.ecommerce.nivora.search.popup.dto.SearchPopupResponse;
import com.ecommerce.nivora.search.popup.service.SearchPopupService;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search Popup", description = "AI Search Popup & Autocomplete suggestions endpoints")
public class SearchPopupController {

    private final SearchPopupService searchPopupService;
    private final UserRepository userRepository;

    @GetMapping("/popup")
    @Operation(summary = "Get default search popup display elements (history, trending, recent viewed, categories, related searches)")
    public ResponseEntity<ApiResponse<SearchPopupResponse>> getSearchPopup(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        Long userId = getAuthenticatedUserId();
        SearchPopupResponse response = searchPopupService.getSearchPopup(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get debounced autocomplete suggestion list based on input query")
    public ResponseEntity<ApiResponse<AutocompleteResponse>> getSuggestions(
            @RequestParam("q") String query,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        Long userId = getAuthenticatedUserId();
        AutocompleteResponse response = searchPopupService.getSuggestions(query, userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/history")
    @Operation(summary = "Delete search history keyword or clear all history")
    public ResponseEntity<ApiResponse<Void>> deleteHistory(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        Long userId = getAuthenticatedUserId();
        searchPopupService.deleteHistory(userId, sessionId, keyword);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                User user = userRepository.findActiveByEmail(auth.getName()).orElse(null);
                if (user != null) {
                    return user.getId();
                }
            } catch (Exception e) {
                // Safe fallback to guest mode
            }
        }
        return null;
    }
}
