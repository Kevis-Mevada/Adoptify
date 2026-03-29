package com.adoptify.backend.controller;

import com.adoptify.backend.dto.request.LoginRequest;
import com.adoptify.backend.dto.request.SignupRequest;
import com.adoptify.backend.dto.request.ForgotPasswordRequest;
import com.adoptify.backend.dto.request.ResetPasswordRequest;
import com.adoptify.backend.dto.response.JwtResponse;
import com.adoptify.backend.dto.response.MessageResponse;
import com.adoptify.backend.model.User;
import com.adoptify.backend.model.UserRole;
import com.adoptify.backend.repository.UserRepository;
import com.adoptify.backend.security.JwtUtils;
import com.adoptify.backend.security.services.UserDetailsImpl;
import com.adoptify.backend.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(item -> item.getAuthority().replace("ROLE_", ""))
                .orElse("");

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getEmail(),
                userRepository.findByEmail(userDetails.getEmail())
                        .map(User::getFullName).orElse(""),
                role));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        String roleStr = signupRequest.getRole();
        if (roleStr == null || roleStr.trim().isEmpty()) {
            roleStr = "REGULAR_USER";
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Invalid role specified!"));
        }

        if (role == UserRole.ADMIN) {
             return ResponseEntity.badRequest()
                     .body(new MessageResponse("Error: Cannot register as ADMIN directly!"));
        }

        if (role == UserRole.NGO) {
            if (signupRequest.getOrganizationName() == null || signupRequest.getOrganizationName().trim().isEmpty() ||
                signupRequest.getLicenseNumber() == null || signupRequest.getLicenseNumber().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: NGO registration requires organizationName and licenseNumber!"));
            }
        }

        User user = User.builder()
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .fullName(signupRequest.getFullName())
                .phone(signupRequest.getPhone())
                .city(signupRequest.getCity())
                .address(signupRequest.getAddress())
                .latitude(signupRequest.getLatitude())
                .longitude(signupRequest.getLongitude())
                .role(role)
                .organizationName(role == UserRole.NGO ? signupRequest.getOrganizationName() : null)
                .licenseNumber(role == UserRole.NGO ? signupRequest.getLicenseNumber() : null)
                .isVerified(false)
                .isActive(true)
                .rescueEnabled(role == UserRole.NGO)
                .build();

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse("User logged out successfully!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.sendResetEmail(request.getEmail());
            // Always return success to prevent email enumeration
            return ResponseEntity.ok(new MessageResponse("If an account exists with this email, you will receive a reset link"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Failed to process request: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam("token") String token) {
        try {
            passwordResetService.validateResetToken(token);
            return ResponseEntity.ok(java.util.Map.of("valid", true, "message", "Token is valid"));
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Map.of("valid", false, "message", e.getMessage()));
        }
    }
}
