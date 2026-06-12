package com.ecommerce.nivora.order.dto.response;


import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShippingAddressResponse {
    private Long id;
    private String recipientName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String countryCode;
    private String trackingNumber;
    private String carrier;
}
