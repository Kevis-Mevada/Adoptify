package com.adoptify.backend.controller;

import com.adoptify.backend.dto.response.AdminStatsResponse;
import com.adoptify.backend.dto.response.MessageResponse;
import com.adoptify.backend.model.AdoptionRequest;
import com.adoptify.backend.model.RescueReport;
import com.adoptify.backend.model.User;
import com.adoptify.backend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/verify")
    public ResponseEntity<?> verifyNGO(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String notes = body != null ? body.get("notes") : "";
            adminService.verifyNGO(id, notes);
            return ResponseEntity.ok(new MessageResponse("NGO verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/reject")
    public ResponseEntity<?> rejectNGO(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        try {
            String reason = body != null ? body.get("reason") : "No reason provided";
            adminService.rejectNGO(id, reason);
            return ResponseEntity.ok(new MessageResponse("NGO rejected successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/rescue-reports")
    public ResponseEntity<List<RescueReport>> getAllRescueReports() {
        return ResponseEntity.ok(adminService.getAllRescueReports());
    }

    @GetMapping("/adoption-requests")
    public ResponseEntity<List<AdoptionRequest>> getAllAdoptionRequests() {
        return ResponseEntity.ok(adminService.getAllAdoptionRequests());
    }

    @PostMapping("/rescue/{id}/rate")
    public ResponseEntity<?> rateRescueNGO(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        try {
            Object ratingObj = body.get("rating");
            Integer rating = null;
            if (ratingObj instanceof Integer) {
                rating = (Integer) ratingObj;
            } else if (ratingObj instanceof String) {
                rating = Integer.parseInt((String) ratingObj);
            }
            String remarks = (String) body.get("remarks");
            adminService.rateRescueNGO(id, rating, remarks);
            return ResponseEntity.ok(new MessageResponse("NGO rated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
