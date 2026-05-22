package com.ecommerce.nivora.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CheckoutRequest {

    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Address is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @Builder.Default
    private String countryCode = "VN";

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String notes;
    private String currency;
}
