package com.ecommerce.nivora.chat.service;

import com.ecommerce.nivora.chat.dto.request.ChatRequest;
import com.ecommerce.nivora.chat.dto.response.ChatResponse;



public interface ChatService {
    ChatResponse getChatResponse(ChatRequest request);
}
