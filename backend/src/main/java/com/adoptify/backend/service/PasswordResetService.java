package com.adoptify.backend.service;

import com.adoptify.backend.model.PasswordResetToken;
import com.adoptify.backend.model.User;
import com.adoptify.backend.repository.PasswordResetTokenRepository;
import com.adoptify.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.reset-token.expiry-hours:24}")
    private int tokenExpiryHours;

    @Value("${app.frontend.url:http://localhost:5500}")
    private String frontendUrl;

    @Transactional
    public PasswordResetToken generateResetToken(User user) {
        // Delete any existing tokens for this user before creating a new one
        tokenRepository.deleteByUser(user);
        tokenRepository.flush(); // Force delete to happen before insert to avoid unique constraint violation

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusHours(tokenExpiryHours))
                .used(false)
                .build();

        return tokenRepository.save(token);
    }

    @Transactional
    public void sendResetEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        // Always return success even if user doesn't exist for security
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            PasswordResetToken token = generateResetToken(user);
            String resetLink = frontendUrl + "/auth/reset-password.html?token=" + token.getToken();
            
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink, user.getFullName());
        }
    }

    public User validateResetToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.isExpired()) {
            throw new RuntimeException("Token has expired");
        }

        if (resetToken.isUsed()) {
            throw new RuntimeException("Token has already been used");
        }

        return resetToken.getUser();
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.isExpired() || resetToken.isUsed()) {
            throw new RuntimeException("Token is invalid or expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM every day
    public void deleteExpiredTokens() {
        tokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
    }
}
