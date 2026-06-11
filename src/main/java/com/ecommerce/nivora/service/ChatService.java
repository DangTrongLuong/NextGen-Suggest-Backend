package com.ecommerce.nivora.service;

import com.ecommerce.nivora.dto.request.ChatRequest;
import com.ecommerce.nivora.dto.response.ChatResponse;

public interface ChatService {
    ChatResponse getChatResponse(ChatRequest request);
}
