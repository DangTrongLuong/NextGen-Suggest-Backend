package com.ecommerce.nivora.user.service;

import com.ecommerce.nivora.exception.ResourceNotFoundException;
import com.ecommerce.nivora.user.dto.request.UserAddressRequest;
import com.ecommerce.nivora.user.dto.response.UserAddressResponse;
import com.ecommerce.nivora.user.entity.User;
import com.ecommerce.nivora.user.entity.UserAddress;
import com.ecommerce.nivora.user.repository.UserRepository;
import com.ecommerce.nivora.user.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserAddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserIdAndDeletedAtIsNull(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserAddressResponse createAddress(Long userId, UserAddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<UserAddress> existing = addressRepository.findByUserIdAndDeletedAtIsNull(userId);
        boolean isDefault = request.getIsDefault() || existing.isEmpty();

        if (isDefault) {
            resetOtherDefaults(userId);
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .addressDetail(request.getAddressDetail())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isDefault(isDefault)
                .label(request.getLabel())
                // Legacy support
                .addressLine1(request.getAddressDetail())
                .city(request.getProvince())
                .postalCode("100000")
                .countryCode("VN")
                .build();

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public UserAddressResponse updateAddress(Long userId, Long addressId, UserAddressRequest request) {
        UserAddress address = addressRepository.findByIdAndUserIdAndDeletedAtIsNull(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAddress", addressId));

        boolean wasDefault = address.getIsDefault();
        boolean isDefault = request.getIsDefault();

        if (isDefault && !wasDefault) {
            resetOtherDefaults(userId);
        } else if (!isDefault && wasDefault) {
            List<UserAddress> existing = addressRepository.findByUserIdAndDeletedAtIsNull(userId);
            if (existing.size() <= 1) {
                isDefault = true;
            }
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setAddressDetail(request.getAddressDetail());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setIsDefault(isDefault);
        address.setLabel(request.getLabel());
        address.setAddressLine1(request.getAddressDetail());
        address.setCity(request.getProvince());

        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = addressRepository.findByIdAndUserIdAndDeletedAtIsNull(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAddress", addressId));

        address.setDeletedAt(LocalDateTime.now());
        addressRepository.save(address);

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            List<UserAddress> active = addressRepository.findByUserIdAndDeletedAtIsNull(userId);
            if (!active.isEmpty()) {
                UserAddress first = active.get(0);
                first.setIsDefault(true);
                addressRepository.save(first);
            }
        }
    }

    @Override
    @Transactional
    public UserAddressResponse setDefaultAddress(Long userId, Long addressId) {
        UserAddress address = addressRepository.findByIdAndUserIdAndDeletedAtIsNull(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAddress", addressId));

        resetOtherDefaults(userId);
        address.setIsDefault(true);
        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional(readOnly = true)
    public UserAddressResponse getDefaultAddress(Long userId) {
        return addressRepository.findByUserIdAndIsDefaultTrueAndDeletedAtIsNull(userId)
                .map(this::toResponse)
                .orElse(null);
    }

    private void resetOtherDefaults(Long userId) {
        List<UserAddress> active = addressRepository.findByUserIdAndDeletedAtIsNull(userId);
        for (UserAddress addr : active) {
            if (Boolean.TRUE.equals(addr.getIsDefault())) {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            }
        }
    }

    private UserAddressResponse toResponse(UserAddress address) {
        if (address == null) return null;
        return UserAddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .addressDetail(address.getAddressDetail())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isDefault(address.getIsDefault())
                .label(address.getLabel())
                .build();
    }
}
