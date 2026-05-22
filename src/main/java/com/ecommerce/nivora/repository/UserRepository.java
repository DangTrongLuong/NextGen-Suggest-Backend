package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUuid(String uuid);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(String email);

    @Query("""
        SELECT u FROM User u
        WHERE u.deletedAt IS NULL
          AND (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    org.springframework.data.domain.Page<User> findAllForAdmin(
        @org.springframework.data.repository.query.Param("search") String search,
        org.springframework.data.domain.Pageable pageable
    );
}
