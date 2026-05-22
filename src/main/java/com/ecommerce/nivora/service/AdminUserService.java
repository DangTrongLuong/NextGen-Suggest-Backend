package com.ecommerce.nivora.service;

import com.ecommerce.nivora.common.PageResponse;
import com.ecommerce.nivora.dto.response.AdminUserResponse;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    PageResponse<AdminUserResponse> getAllUsers(String search, Pageable pageable);
    AdminUserResponse getUserById(Long id);
    AdminUserResponse setActive(Long id, boolean active);
}
