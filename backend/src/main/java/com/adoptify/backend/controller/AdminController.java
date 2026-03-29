package com.adoptify.backend.controller;

import com.adoptify.backend.model.User;
import com.adoptify.backend.dto.request.RejectRequest; // Added import
import com.adoptify.backend.repository.AnimalRepository;
import com.adoptify.backend.repository.RescueReportRepository;
import com.adoptify.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private RescueReportRepository rescueReportRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalAnimals", animalRepository.count());
        stats.put("totalRescues", rescueReportRepository.count());
        stats.put("pendingNGOs", (long) userRepository.findByIsVerifiedFalse().size());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/users/{id}/verify")
    public ResponseEntity<?> verifyUser(@PathVariable("id") Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsVerified(true);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/users/{id}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable("id") Long id, @RequestBody RejectRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsVerified(false);
        user.setIsActive(false); // Can also mark rejected, but this is simple MVP approach
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable("id") Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(user.getIsActive() != null ? !user.getIsActive() : false);
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User request) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getRole() != null) user.setRole(request.getRole());
        return ResponseEntity.ok(userRepository.save(user));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        try {
            userRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete user. They have active animals or reports tied to their account. Please suspend them instead."));
        }
    }
}
