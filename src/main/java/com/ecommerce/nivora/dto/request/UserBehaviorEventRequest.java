package com.ecommerce.nivora.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorEventRequest {

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Event type is required")
    private String eventType;

    private Long productId;

    private String queryText;
}
