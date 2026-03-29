package com.adoptify.backend.controller;

import com.adoptify.backend.dto.request.AdoptionRequestDTO;
import com.adoptify.backend.dto.request.RejectRequestDTO;
import com.adoptify.backend.dto.response.AdoptionResponseDTO;
import com.adoptify.backend.dto.response.MessageResponse;
import com.adoptify.backend.model.RequestStatus;
import com.adoptify.backend.model.User;
import com.adoptify.backend.repository.UserRepository;
import com.adoptify.backend.security.services.UserDetailsImpl;
import com.adoptify.backend.service.AdoptionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/adoptions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AdoptionController {

    @Autowired
    private AdoptionService adoptionService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AdoptionResponseDTO> createRequest(@Valid @RequestBody AdoptionRequestDTO request,
                                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User adopter = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(adoptionService.createAdoptionRequest(request, adopter));
    }

    @GetMapping("/received")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AdoptionResponseDTO>> getReceivedRequests(
            @RequestParam(name = "status", required = false) String status,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        RequestStatus requestStatus = null;
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            requestStatus = RequestStatus.valueOf(status.toUpperCase());
        }
        return ResponseEntity.ok(adoptionService.getRequestsForOwner(userDetails.getId(), requestStatus));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AdoptionResponseDTO>> getMyRequests(
            @RequestParam(name = "status", required = false) String status,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        RequestStatus requestStatus = null;
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            requestStatus = RequestStatus.valueOf(status.toUpperCase());
        }
        return ResponseEntity.ok(adoptionService.getMyRequests(userDetails.getId(), requestStatus));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AdoptionResponseDTO> approveRequest(@PathVariable("id") Long id,
                                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        return ResponseEntity.ok(adoptionService.approveRequest(id, user));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AdoptionResponseDTO> rejectRequest(@PathVariable("id") Long id,
                                                              @Valid @RequestBody RejectRequestDTO rejectRequest,
                                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId()).orElseThrow();
        return ResponseEntity.ok(adoptionService.rejectRequest(id, rejectRequest.getRejectionReason(), user));
    }

    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> cancelRequest(@PathVariable("id") Long id,
                                                          @AuthenticationPrincipal UserDetailsImpl userDetails) {
        adoptionService.cancelRequest(id, userDetails.getId());
        return ResponseEntity.ok(new MessageResponse("Request cancelled successfully"));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> completeAdoption(@PathVariable("id") Long id,
                                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        adoptionService.completeAdoption(id, userDetails.getId());
        return ResponseEntity.ok(new MessageResponse("Adoption completed successfully"));
    }

    @GetMapping("/check/{animalId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkIfAlreadyRequested(@PathVariable("animalId") Long animalId,
                                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean alreadyRequested = adoptionService.checkIfAlreadyRequested(animalId, userDetails.getId());
        // Simple response as requested
        return ResponseEntity.ok(Map.of("alreadyRequested", alreadyRequested));
    }
}
