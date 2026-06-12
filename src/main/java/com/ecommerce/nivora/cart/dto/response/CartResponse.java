package com.ecommerce.nivora.cart.dto.response;


import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartResponse {
    private Long id;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private int itemCount;
}
