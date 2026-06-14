package com.ecommerce.nivora.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String province;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String district;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String addressDetail;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    private Boolean isDefault = false;

    private String label;
}
