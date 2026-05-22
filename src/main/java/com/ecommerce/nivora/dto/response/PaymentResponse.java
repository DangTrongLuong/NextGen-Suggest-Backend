package com.ecommerce.nivora.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResponse {
    private Long id;
    private String method;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String gatewayTransactionId;
    private LocalDateTime paidAt;
}
