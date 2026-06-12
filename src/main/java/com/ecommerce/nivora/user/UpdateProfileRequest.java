package com.ecommerce.nivora.user;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String name;
    private String phone;
    private String avatarUrl;
    private Integer age;
    private String gender;
    private String occupation;
    private BigDecimal income;
    private String maritalStatus;
}
