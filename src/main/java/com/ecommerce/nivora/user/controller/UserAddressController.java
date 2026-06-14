package com.ecommerce.nivora.user.controller;

import com.ecommerce.nivora.common.dto.response.ApiResponse;
import com.ecommerce.nivora.security.utils.SecurityUtils;
import com.ecommerce.nivora.user.dto.request.UserAddressRequest;
import com.ecommerce.nivora.user.dto.response.UserAddressResponse;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-addresses")
@RequiredArgsConstructor
@Tag(name = "User Addresses", description = "User shipping address endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserAddressController {

    private final UserAddressService addressService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get all user addresses")
    public ResponseEntity<ApiResponse<List<UserAddressResponse>>> getAddresses() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(addressService.getAddresses(user.getId())));
    }

    @GetMapping("/default")
    @Operation(summary = "Get default user address")
    public ResponseEntity<ApiResponse<UserAddressResponse>> getDefaultAddress() {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(addressService.getDefaultAddress(user.getId())));
    }

    @PostMapping
    @Operation(summary = "Create a new user address")
    public ResponseEntity<ApiResponse<UserAddressResponse>> createAddress(@Valid @RequestBody UserAddressRequest request) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo địa chỉ thành công",
                addressService.createAddress(user.getId(), request)
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user address")
    public ResponseEntity<ApiResponse<UserAddressResponse>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody UserAddressRequest request
    ) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật địa chỉ thành công",
                addressService.updateAddress(user.getId(), id, request)
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        addressService.deleteAddress(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Xóa địa chỉ thành công", null));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Set user address as default")
    public ResponseEntity<ApiResponse<UserAddressResponse>> setDefaultAddress(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "Đặt địa chỉ mặc định thành công",
                addressService.setDefaultAddress(user.getId(), id)
        ));
    }
}
