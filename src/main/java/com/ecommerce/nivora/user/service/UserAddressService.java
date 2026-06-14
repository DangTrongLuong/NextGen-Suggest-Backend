package com.ecommerce.nivora.user.service;

import com.ecommerce.nivora.user.dto.request.UserAddressRequest;
import com.ecommerce.nivora.user.dto.response.UserAddressResponse;

import java.util.List;

public interface UserAddressService {
    List<UserAddressResponse> getAddresses(Long userId);
    UserAddressResponse createAddress(Long userId, UserAddressRequest request);
    UserAddressResponse updateAddress(Long userId, Long addressId, UserAddressRequest request);
    void deleteAddress(Long userId, Long addressId);
    UserAddressResponse setDefaultAddress(Long userId, Long addressId);
    UserAddressResponse getDefaultAddress(Long userId);
}
