package com.ecommerce.nivora.controller;

import com.ecommerce.nivora.common.ApiResponse;
import com.ecommerce.nivora.dto.request.ChatRequest;
import com.ecommerce.nivora.dto.response.ChatResponse;
import com.ecommerce.nivora.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/chat", "/api/chat"})
@RequiredArgsConstructor
@Tag(name = "Chat", description = "AI Shopping Assistant chat endpoints")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Get response from AI Shopping Assistant Chatbot")
    public ResponseEntity<ApiResponse<ChatResponse>> getChatResponse(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.getChatResponse(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
