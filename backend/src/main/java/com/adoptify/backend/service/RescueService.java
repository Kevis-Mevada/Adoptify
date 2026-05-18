package com.adoptify.backend.service;

import com.adoptify.backend.dto.request.RescueReportDTO;
import com.adoptify.backend.model.*;
import com.adoptify.backend.repository.RescueReportRepository;
import com.adoptify.backend.repository.RescueResponseRepository;
import com.adoptify.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.adoptify.backend.exception.ResourceNotFoundException;
import com.adoptify.backend.exception.UnauthorizedException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class RescueService {

    private static final Logger logger = LoggerFactory.getLogger(RescueService.class);

    private static final Double DEFAULT_RESCUE_RADIUS_KM = 25.0;

    @Autowired
    private RescueReportRepository rescueReportRepository;

    @Autowired
    private RescueResponseRepository rescueResponseRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AIImageDetectionService aiImageDetectionService;

    @Transactional
    public RescueReport createRescueReport(RescueReportDTO request, User reporter, List<org.springframework.web.multipart.MultipartFile> imageFiles) {
        // Enforce images if required
        if (request.isImagesRequired() || true) { // Policy: Always required for rescue now
            aiImageDetectionService.validateRescueImages(imageFiles);
        }

        RescueReport report = RescueReport.builder()
                .reporter(reporter)
                .reporterName(request.getReporterName() != null ? request.getReporterName() : (reporter != null ? reporter.getFullName() : null))
                .reporterPhone(request.getReporterPhone() != null ? request.getReporterPhone() : (reporter != null ? reporter.getPhone() : null))
                .animalType(request.getAnimalType())
                .animalCount(request.getAnimalCount())
                .animalCondition(request.getAnimalCondition())
                .emergencyLevel(request.getEmergencyLevel())
                .locationAddress(request.getLocationAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .landmark(request.getLandmark())
                .description(request.getDescription())
                .status(RescueStatus.PENDING)
                .build();

        // Perform AI Detailed Check on the first image for record keeping
        if (!imageFiles.isEmpty()) {
            AIImageDetectionService.DetectionResult result = aiImageDetectionService.detectAIImage(imageFiles.get(0));
            report.setIsImageVerified(!result.isAI());
            report.setImageVerificationScore(result.getConfidenceScore());
            report.setImageVerificationMessage(result.getMessage());
        }

        RescueReport saved = rescueReportRepository.save(report);

        // Upload and save image URLs
        addImagesToReport(saved.getId(), imageFiles);

        // Send alerts to nearby rescuers
        sendRescueAlerts(saved);

        return saved;
    }

    @Transactional
    public void addImagesToReport(Long reportId, List<org.springframework.web.multipart.MultipartFile> files) {
        RescueReport report = rescueReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Rescue report not found"));
        
        StringBuilder imageUrls = new StringBuilder();
        if (report.getImages() != null && !report.getImages().isEmpty()) {
            imageUrls.append(report.getImages());
        }

        for (org.springframework.web.multipart.MultipartFile file : files) {
            String url = fileStorageService.uploadFile(file, "rescue");
            if (imageUrls.length() > 0) imageUrls.append(",");
            imageUrls.append(url);
        }

        report.setImages(imageUrls.toString());
        rescueReportRepository.save(report);
    }

    public List<RescueReport> getNearbyPendingRescues(Double latitude, Double longitude, Double radius, Long currentUserId) {
        // If no location provided, return ALL pending rescues (fallback)
        List<RescueReport> reports;
        if (latitude == null || longitude == null || latitude == 0.0) {
            reports = rescueReportRepository.findByStatus(RescueStatus.PENDING);
        } else {
            double radiusKm = radius != null ? radius : DEFAULT_RESCUE_RADIUS_KM;
            double latDelta = radiusKm / 6371.0 * (180.0 / Math.PI);
            double lngDelta = radiusKm / (6371.0 * Math.cos(Math.toRadians(latitude))) * (180.0 / Math.PI);

            reports = rescueReportRepository.findNearbyPendingReports(
                latitude - latDelta, latitude + latDelta,
                longitude - lngDelta, longitude + lngDelta);
        }

        // Filter out reports the user has already responded to (Accepted or Rejected)
        if (currentUserId != null) {
            List<Long> respondedReportIds = rescueResponseRepository.findByResponderId(currentUserId).stream()
                    .map(res -> res.getRescueReport().getId())
                    .toList();
            
            return reports.stream()
                    .filter(r -> !respondedReportIds.contains(r.getId()))
                    .toList();
        }

        return reports;
    }

    @Transactional
    public RescueResponse acceptRescueRequest(Long reportId, User responder, LocalDateTime estimatedArrival) {
        RescueReport report = rescueReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Rescue report not found"));

        // Only NGOs can accept rescue reports
        if (responder.getRole() != UserRole.NGO) {
            throw new RuntimeException("Only verified NGOs can accept rescue requests");
        }

        // Only verified NGOs can accept rescue reports
        if (responder.getIsVerified() == null || !responder.getIsVerified()) {
            throw new RuntimeException("NGO account pending verification");
        }

        // Check if responder already responded
        rescueResponseRepository.findByRescueReportIdAndResponderId(reportId, responder.getId())
                .ifPresent(existing -> {
                    throw new RuntimeException("You have already responded to this rescue report");
                });

        ResponderType responderType = mapRoleToResponderType(responder.getRole());

        RescueResponse response = RescueResponse.builder()
                .rescueReport(report)
                .responder(responder)
                .responderType(responderType)
                .responseStatus(ResponseStatus.ACCEPTED)
                .estimatedArrivalTime(estimatedArrival)
                .build();

        // Update report status to ASSIGNED
        report.setStatus(RescueStatus.ASSIGNED);
        rescueReportRepository.save(report);

        RescueResponse savedResponse = rescueResponseRepository.save(response);
        
        // Notify reporter via email
        try {
            emailService.sendRescueAcceptedEmailToReporter(report, responder, estimatedArrival);
        } catch (Exception e) {
            logger.error("Failed to send acceptance email: {}", e.getMessage());
        }

        return savedResponse;
    }

    @Transactional
    public RescueResponse rejectRescueRequest(Long reportId, User responder, String reason) {
        RescueReport report = rescueReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Rescue report not found"));

        ResponderType responderType = mapRoleToResponderType(responder.getRole());

        RescueResponse response = RescueResponse.builder()
                .rescueReport(report)
                .responder(responder)
                .responderType(responderType)
                .responseStatus(ResponseStatus.REJECTED)
                .responseMessage(reason)
                .build();

        return rescueResponseRepository.save(response);
    }

    @Transactional
    public RescueReport updateRescueStatus(Long reportId, RescueStatus status) {
        RescueReport report = rescueReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Rescue report not found"));

        report.setStatus(status);
        if (status == RescueStatus.COMPLETED) {
            report.setResolvedAt(LocalDateTime.now());
            // Also update the mission response status to COMPLETED
            rescueResponseRepository.findByRescueReportIdAndResponseStatus(reportId, ResponseStatus.ACCEPTED)
                    .stream().findFirst().ifPresent(res -> {
                        res.setResponseStatus(ResponseStatus.COMPLETED);
                        res.setUpdatedAt(LocalDateTime.now());
                        rescueResponseRepository.save(res);

                        // Notify Reporter
                        try {
                            emailService.sendRescueCompletedEmailToReporter(report, res.getResponder());
                        } catch (Exception e) {
                            logger.error("Failed to send rescue completion email: {}", e.getMessage());
                        }
                    });
        } else if (status == RescueStatus.RESCUED) {
            report.setResolvedAt(LocalDateTime.now());
        }

        return rescueReportRepository.save(report);
    }

    @Transactional
    public RescueReport rateRescue(Long reportId, Integer rating, String remarks) {
        RescueReport report = rescueReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Rescue report not found"));

        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        report.setRating(rating);
        report.setAdminRemarks(remarks);

        return rescueReportRepository.save(report);
    }

    public void sendRescueAlerts(RescueReport report) {
        // Find ALL verified NGOs as per user requirements
        List<User> verifiedNGOs = userRepository.findByRoleAndIsVerifiedTrue(UserRole.NGO);

        logger.info("Found {} verified NGOs to notify for rescue report {}", verifiedNGOs.size(), report.getId());

        for (User ngo : verifiedNGOs) {
            try {
                logger.info("Sending rescue alert email to NGO: {} ({})", ngo.getFullName(), ngo.getEmail());
                emailService.sendRescueAlertEmail(
                        ngo.getEmail(),
                        ngo.getFullName(),
                        report.getAnimalType(),
                        report.getAnimalCondition().name(),
                        report.getEmergencyLevel().name(),
                        report.getLocationAddress(),
                        report.getLandmark(),
                        report.getReporterName(),
                        report.getReporterPhone(),
                        report.getDescription());
            } catch (Exception e) {
                logger.error("Failed to send rescue alert to NGO {}: {}", ngo.getEmail(), e.getMessage());
            }
        }
    }

    public List<RescueReport> getReportsByReporter(Long reporterId) {
        return rescueReportRepository.findByReporterId(reporterId);
    }

    @Transactional(readOnly = true)
    public Page<RescueReportDTO> getMyReports(Long userId, List<RescueStatus> statuses, Pageable pageable) {
        Page<RescueReport> reportPage;
        if (statuses != null && !statuses.isEmpty()) {
            reportPage = rescueReportRepository.findByReporterIdAndStatusIn(userId, statuses, pageable);
        } else {
            reportPage = rescueReportRepository.findByReporterId(userId, pageable);
        }
        return reportPage.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public RescueReportDTO getReportById(Long reportId, User currentUser) {
        RescueReport report = rescueReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue report not found"));

        // Authorization check: Show details if user is reporter, admin, assigned responder, or any NGO (for pending)
        boolean isReporter = report.getReporter() != null && report.getReporter().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isNGO = currentUser.getRole() == UserRole.NGO;
        
        boolean isAssigned = false;
        if (!isReporter && !isAdmin) {
            isAssigned = rescueResponseRepository.findByRescueReportId(reportId).stream()
                .anyMatch(r -> r.getResponder().getId().equals(currentUser.getId()) && r.getResponseStatus() == ResponseStatus.ACCEPTED);
        }

        if (!isReporter && !isAdmin && !isAssigned) {
            // Allow NGOs to view PENDING reports so they can decide to accept
            if (!(isNGO && report.getStatus() == RescueStatus.PENDING)) {
                throw new UnauthorizedException("You are not authorized to view this report details");
            }
        }

        return convertToDTO(report);
    }

    @Transactional(readOnly = true)
    public RescueResponse getResponseForReport(Long reportId, User currentUser) {
        // Find the accepted response
        return rescueResponseRepository.findByRescueReportIdAndResponseStatus(reportId, ResponseStatus.ACCEPTED)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private RescueReportDTO convertToDTO(RescueReport report) {
        List<String> imageUrls = report.getImages() != null ? 
                Arrays.asList(report.getImages().split(",")) : 
                java.util.Collections.emptyList();

        RescueReportDTO dto = RescueReportDTO.builder()
                .id(report.getId())
                .animalType(report.getAnimalType())
                .animalCount(report.getAnimalCount())
                .animalCondition(report.getAnimalCondition())
                .emergencyLevel(report.getEmergencyLevel())
                .locationAddress(report.getLocationAddress())
                .latitude(report.getLatitude())
                .longitude(report.getLongitude())
                .landmark(report.getLandmark())
                .description(report.getDescription())
                .imageUrls(imageUrls)
                .status(report.getStatus())
                .reporterName(report.getReporterName())
                .reporterPhone(report.getReporterPhone())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();

        // Check for assigned responder
        rescueResponseRepository.findByRescueReportIdAndResponseStatus(report.getId(), ResponseStatus.ACCEPTED)
                .stream().findFirst().ifPresent(response -> {
                    User responder = response.getResponder();
                    dto.setResponder(RescueReportDTO.ResponderInfo.builder()
                            .name(responder.getFullName())
                            .phone(responder.getPhone())
                            .organizationName(responder.getOrganizationName())
                            .estimatedArrivalTime(response.getEstimatedArrivalTime())
                            .build());
                });

        return dto;
    }

    public List<RescueResponse> getMyAcceptedRescues(Long ngoId, ResponseStatus status) {
        return rescueResponseRepository.findByResponderIdAndResponseStatus(ngoId, status);
    }

    public java.util.Map<String, Object> getNGOStats(Long ngoId) {
        User ngo = userRepository.findById(ngoId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO not found"));
                
        List<RescueResponse> allResponses = rescueResponseRepository.findByResponderId(ngoId);
        
        // Include both active (ACCEPTED) and finished (COMPLETED) missions
        List<RescueResponse> acceptedAndCompleted = allResponses.stream()
                .filter(r -> r.getResponseStatus() == ResponseStatus.ACCEPTED || r.getResponseStatus() == ResponseStatus.COMPLETED)
                .toList();

        long completedCount = acceptedAndCompleted.stream()
                .filter(r -> r.getResponseStatus() == ResponseStatus.COMPLETED)
                .count();

        double avgRating = acceptedAndCompleted.stream()
                .filter(r -> r.getRescueReport().getRating() != null)
                .mapToDouble(r -> r.getRescueReport().getRating())
                .average()
                .orElse(0.0);

        // Pending alerts nearby (approximate for stats)
        double lat = ngo.getLatitude() != null ? ngo.getLatitude() : 0.0;
        double lng = ngo.getLongitude() != null ? ngo.getLongitude() : 0.0;
        
        long nearbyAlerts = rescueReportRepository.findAll().stream()
                .filter(r -> r.getStatus() == RescueStatus.PENDING)
                .filter(r -> {
                    // Don't count if already responded (Accepted/Rejected)
                    boolean alreadyResponded = allResponses.stream()
                            .anyMatch(res -> res.getRescueReport().getId().equals(r.getId()));
                    if (alreadyResponded) return false;

                    if (lat == 0.0 || lng == 0.0) return true; // Show all if NGO has no location
                    return calculateDistance(lat, lng, r.getLatitude(), r.getLongitude()) <= 50.0;
                })
                .count();

        return java.util.Map.of(
            "totalAccepted", acceptedAndCompleted.size(),
            "completedCount", completedCount,
            "pendingAlerts", nearbyAlerts,
            "averageRating", String.format("%.1f", avgRating)
        );
    }

    private ResponderType mapRoleToResponderType(UserRole role) {
        if (role == UserRole.NGO) {
            return ResponderType.NGO;
        }
        return ResponderType.RESCUER;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
