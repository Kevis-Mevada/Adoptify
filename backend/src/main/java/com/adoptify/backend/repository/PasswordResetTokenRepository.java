package com.adoptify.backend.repository;

import com.adoptify.backend.model.PasswordResetToken;
import com.adoptify.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now")
    void deleteAllExpiredTokens(@Param("now") LocalDateTime now);
}
