package com.ecommerce.nivora.repository;

import com.ecommerce.nivora.entity.RefreshToken;
import com.ecommerce.nivora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = CURRENT_TIMESTAMP WHERE r.user = :user AND r.revokedAt IS NULL")
    void revokeAllByUser(User user);
}
