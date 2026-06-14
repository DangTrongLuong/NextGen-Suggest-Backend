package com.ecommerce.nivora.user.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAddressResponse {
    private Long id;
    private String recipientName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    private Double latitude;
    private Double longitude;
    private Boolean isDefault;
    private String label;
}
