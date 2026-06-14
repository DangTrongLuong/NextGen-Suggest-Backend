package com.ecommerce.nivora.user.repository;

import com.ecommerce.nivora.user.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUserIdAndDeletedAtIsNull(Long userId);
    Optional<UserAddress> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
    Optional<UserAddress> findByUserIdAndIsDefaultTrueAndDeletedAtIsNull(Long userId);
}
