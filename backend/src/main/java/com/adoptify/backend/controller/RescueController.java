package com.adoptify.backend.controller;

import com.adoptify.backend.dto.request.RescueReportDTO;
import com.adoptify.backend.model.RescueReport;
import com.adoptify.backend.model.RescueResponse;
import com.adoptify.backend.model.User;
import com.adoptify.backend.repository.UserRepository;
import com.adoptify.backend.service.RescueService;
import com.adoptify.backend.service.AIImageDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.adoptify.backend.model.RescueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequestMapping("/api/rescue")
public class RescueController {

    @Autowired
    private RescueService rescueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIImageDetectionService aiImageDetectionService;

    @PostMapping(value = "/report", consumes = { "multipart/form-data" })
    public ResponseEntity<RescueReport> createReport(
            @RequestPart("report") RescueReportDTO request,
            @RequestPart("files") List<MultipartFile> files,
            Authentication authentication) {

        User reporter = null;
        if (authentication != null && authentication.getName() != null
                && !authentication.getName().equals("anonymousUser")) {
            reporter = userRepository.findByEmail(authentication.getName()).orElse(null);
        }
        return ResponseEntity.ok(rescueService.createRescueReport(request, reporter, files));
    }

    @PostMapping("/validate-image")
    public ResponseEntity<?> validateImage(@RequestParam("file") MultipartFile file) {
        AIImageDetectionService.DetectionResult result = aiImageDetectionService.detectAIImage(file);
        return ResponseEntity.ok(java.util.Map.of(
                "valid", !result.isAI(),
                "isAI", result.isAI(),
                "message", result.getMessage()));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<RescueReport>> getNearbyReports(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam(value = "radius", required = false) Double radius,
            Authentication authentication) {
        
        Long userId = null;
        if (authentication != null && !"anonymousUser".equals(authentication.getName())) {
            userId = userRepository.findByEmail(authentication.getName()).map(User::getId).orElse(null);
        }
        
        return ResponseEntity.ok(rescueService.getNearbyPendingRescues(lat, lng, radius, userId));
    }


    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<RescueResponse> acceptRescue(@PathVariable("id") Long id, 
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {
        User responder = userRepository.findByEmail(authentication.getName()).orElseThrow();
        java.time.LocalDateTime eta = java.time.LocalDateTime.parse(body.get("estimatedArrivalTime"));
        return ResponseEntity.ok(rescueService.acceptRescueRequest(id, responder, eta));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<RescueResponse> rejectRescue(@PathVariable("id") Long id, 
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {
        User responder = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(rescueService.rejectRescueRequest(id, responder, body.get("reason")));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<?> uploadImages(@PathVariable("id") Long id,
            @RequestParam("files") List<MultipartFile> files) {
        rescueService.addImagesToReport(id, files);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-accepted")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<List<RescueResponse>> getMyAccepted(
            @RequestParam(name = "status", defaultValue = "ACCEPTED") com.adoptify.backend.model.ResponseStatus status,
            Authentication authentication) {
        User ngo = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(rescueService.getMyAcceptedRescues(ngo.getId(), status));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<RescueReport> updateRescueStatus(@PathVariable("id") Long id,
            @RequestBody java.util.Map<String, String> body) {
        RescueStatus status = RescueStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(rescueService.updateRescueStatus(id, status));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<java.util.Map<String, Object>> getNgoStats(Authentication authentication) {
        User ngo = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(rescueService.getNGOStats(ngo.getId()));
    }

    @GetMapping("/my-reports")
    public ResponseEntity<Page<RescueReportDTO>> getMyReports(
            @RequestParam(name = "status", required = false) List<RescueStatus> statuses,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(rescueService.getMyReports(user.getId(), statuses, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RescueReportDTO> getReportById(
            @PathVariable(name = "id") Long id,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(rescueService.getReportById(id, user));
    }

    @GetMapping("/{id}/response")
    public ResponseEntity<RescueResponse> getReportResponse(
            @PathVariable(name = "id") Long id,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(rescueService.getResponseForReport(id, user));
    }
}
