package com.ecommerce.nivora.order.dto.response;

import com.ecommerce.nivora.payment.dto.response.PaymentResponse;


import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String notes;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private List<OrderItemResponse> items;
    private ShippingAddressResponse shippingAddress;
    private PaymentResponse payment;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
}
