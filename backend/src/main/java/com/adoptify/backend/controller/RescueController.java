package com.adoptify.backend.controller;

import com.adoptify.backend.dto.request.RescueReportDTO;
import com.adoptify.backend.model.RescueReport;
import com.adoptify.backend.model.RescueResponse;
import com.adoptify.backend.model.User;
import com.adoptify.backend.repository.UserRepository;
import com.adoptify.backend.service.RescueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/rescue")
public class RescueController {

    @Autowired
    private RescueService rescueService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/report")
    public ResponseEntity<RescueReport> createReport(@RequestBody RescueReportDTO request, Authentication authentication) {
        User reporter = null;
        if (authentication != null && authentication.getName() != null && !authentication.getName().equals("anonymousUser")) {
            reporter = userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        return ResponseEntity.ok(rescueService.createRescueReport(request, reporter));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<RescueReport>> getNearbyReports(
            @RequestParam("lat") Double lat, 
            @RequestParam("lng") Double lng, 
            @RequestParam(value = "radius", required = false) Double radius) {
        return ResponseEntity.ok(rescueService.getNearbyPendingRescues(lat, lng, radius));
    }

    @GetMapping("/my-reports")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RescueReport>> getMyReports(Authentication authentication) {
        User reporter = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(rescueService.getReportsByReporter(reporter.getId()));
    }

    @PostMapping("/{id}/respond")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<RescueResponse> acceptResponse(@PathVariable("id") Long id, Authentication authentication) {
        User responder = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(rescueService.acceptRescueRequest(id, responder));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<?> uploadImage(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file) {
        // Implementation for MVP - Accept image blindly
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-responses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RescueResponse>> getMyResponses(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        // Mock method because there is no RescueResponseService method for this
        // For MVP, if it crashes frontend just return empty list
        return ResponseEntity.ok(List.of());
    }

    @PutMapping("/responses/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateResponseStatus(@PathVariable("id") Long id, @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok().build();
    }
}
