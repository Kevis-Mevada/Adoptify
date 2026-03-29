package com.adoptify.backend.service;

import com.adoptify.backend.dto.request.NotificationRequest;
import com.adoptify.backend.model.Notification;
import com.adoptify.backend.model.User;
import com.adoptify.backend.repository.NotificationRepository;
import com.adoptify.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public Notification createNotification(NotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedId(request.getRelatedId())
                .isRead(false)
                .isEmailSent(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Send email if requested
        if (request.isSendEmail()) {
            sendEmailNotification(saved);
        }

        return saved;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    public void sendEmailNotification(Notification notification) {
        try {
            User user = notification.getUser();
            emailService.sendSimpleMessage(
                    user.getEmail(),
                    notification.getTitle(),
                    notification.getMessage());

            notification.setIsEmailSent(true);
            notificationRepository.save(notification);
            logger.info("Email notification sent to user {}", user.getId());
        } catch (Exception e) {
            logger.error("Failed to send email notification: {}", e.getMessage());
        }
    }

    /**
     * Scheduled job to delete notifications older than 30 days.
     * Runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteByCreatedAtBefore(cutoffDate);
        logger.info("Deleted notifications older than {}", cutoffDate);
    }
}
