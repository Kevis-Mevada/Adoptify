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

import java.time.LocalDateTime;
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
    private LocationService locationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public RescueReport createRescueReport(RescueReportDTO request, User reporter) {
        RescueReport report = RescueReport.builder()
                .reporter(reporter)
                .reporterName(request.getReporterName() != null ? request.getReporterName() : reporter.getFullName())
                .reporterPhone(request.getReporterPhone() != null ? request.getReporterPhone() : reporter.getPhone())
                .animalType(request.getAnimalType())
                .animalCount(request.getAnimalCount())
                .animalCondition(request.getAnimalCondition())
                .emergencyLevel(request.getEmergencyLevel())
                .locationAddress(request.getLocationAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .landmark(request.getLandmark())
                .description(request.getDescription())
                .images(request.getImages())
                .status(RescueStatus.PENDING)
                .build();

        RescueReport saved = rescueReportRepository.save(report);

        // Send alerts to nearby rescuers
        sendRescueAlerts(saved);

        return saved;
    }

    public List<RescueReport> getNearbyPendingRescues(Double latitude, Double longitude, Double radius) {
        double radiusKm = radius != null ? radius : DEFAULT_RESCUE_RADIUS_KM;

        double latDelta = radiusKm / 6371.0 * (180.0 / Math.PI);
        double lngDelta = radiusKm / (6371.0 * Math.cos(Math.toRadians(latitude))) * (180.0 / Math.PI);

        return rescueReportRepository.findNearbyPendingReports(
                latitude - latDelta, latitude + latDelta,
                longitude - lngDelta, longitude + lngDelta);
    }

    @Transactional
    public RescueResponse acceptRescueRequest(Long reportId, User responder) {
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
                .build();

        // Update report status to ASSIGNED
        report.setStatus(RescueStatus.ASSIGNED);
        rescueReportRepository.save(report);

        return rescueResponseRepository.save(response);
    }

    @Transactional
    public RescueResponse rejectRescueRequest(Long reportId, User responder) {
        RescueReport report = rescueReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Rescue report not found"));

        ResponderType responderType = mapRoleToResponderType(responder.getRole());

        RescueResponse response = RescueResponse.builder()
                .rescueReport(report)
                .responder(responder)
                .responderType(responderType)
                .responseStatus(ResponseStatus.REJECTED)
                .build();

        return rescueResponseRepository.save(response);
    }

    @Transactional
    public RescueReport updateRescueStatus(Long reportId, RescueStatus status) {
        RescueReport report = rescueReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Rescue report not found"));

        report.setStatus(status);

        if (status == RescueStatus.COMPLETED || status == RescueStatus.RESCUED) {
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

    private ResponderType mapRoleToResponderType(UserRole role) {
        if (role == UserRole.NGO) {
            return ResponderType.NGO;
        }
        return ResponderType.RESCUER;
    }
}
