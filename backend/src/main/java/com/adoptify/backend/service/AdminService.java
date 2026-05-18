package com.adoptify.backend.service;

import com.adoptify.backend.dto.response.AdminStatsResponse;
import com.adoptify.backend.model.*;
import com.adoptify.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private RescueReportRepository rescueReportRepository;

    @Autowired
    private AdoptionRequestRepository adoptionRequestRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalNGOs(userRepository.countByRole(UserRole.NGO))
                .pendingNGOs(userRepository.countByRoleAndIsVerified(UserRole.NGO, false))
                .totalAnimals(animalRepository.count())
                .totalRescues(rescueReportRepository.count())
                .totalAdoptions(adoptionRequestRepository.countByRequestStatus(RequestStatus.COMPLETED)) // Assuming COMPLETED or simply count all APPROVED
                .build();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete dependencies that are safe to remove
        passwordResetTokenRepository.deleteByUser(user);

        // Check for dependencies that should block deletion to prevent data loss
        if (animalRepository.countByOwner(user) > 0) {
            throw new RuntimeException("Cannot delete user: They have active animal listings.");
        }
        if (rescueReportRepository.countByReporter(user) > 0) {
            throw new RuntimeException("Cannot delete user: They have associated rescue reports.");
        }

        userRepository.delete(user);
    }

    @Transactional
    public void verifyNGO(Long id, String notes) {
        User ngo = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("NGO not found"));
        
        if (ngo.getRole() != UserRole.NGO) {
            throw new RuntimeException("User is not an NGO");
        }

        ngo.setIsVerified(true);
        ngo.setRescueEnabled(true);
        ngo.setVerificationRemarks(notes);
        userRepository.save(ngo);

        emailService.sendNGOVerificationEmail(ngo);
    }

    @Transactional
    public void rejectNGO(Long id, String reason) {
        User ngo = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("NGO not found"));
        
        if (ngo.getRole() != UserRole.NGO) {
            throw new RuntimeException("User is not an NGO");
        }

        emailService.sendNGORejectionEmail(ngo, reason);
        
        // Delete dependencies that are safe to remove
        passwordResetTokenRepository.deleteByUser(ngo);

        // Delete the user completely as requested
        userRepository.delete(ngo);
    }

    public List<RescueReport> getAllRescueReports() {
        return rescueReportRepository.findAll();
    }

    public List<AdoptionRequest> getAllAdoptionRequests() {
        return adoptionRequestRepository.findAll();
    }

    @Transactional
    public void rateRescueNGO(Long rescueId, Integer rating, String remarks) {
        RescueReport report = rescueReportRepository.findById(rescueId)
                .orElseThrow(() -> new RuntimeException("Rescue report not found"));
        
        if (report.getStatus() != RescueStatus.COMPLETED) {
            throw new RuntimeException("Can only rate completed rescues");
        }

        report.setRating(rating);
        report.setRatingRemarks(remarks);
        rescueReportRepository.save(report);
    }
}
