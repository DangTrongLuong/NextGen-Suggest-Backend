package com.ecommerce.nivora.search.popup.service;

import com.ecommerce.nivora.search.popup.dto.AutocompleteResponse;
import com.ecommerce.nivora.search.popup.dto.SearchPopupResponse;

public interface SearchPopupService {
    SearchPopupResponse getSearchPopup(Long userId, String sessionId);
    AutocompleteResponse getSuggestions(String query, Long userId, String sessionId);
    void deleteHistory(Long userId, String sessionId, String keyword);
}
