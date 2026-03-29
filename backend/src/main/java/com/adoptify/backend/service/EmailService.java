package com.adoptify.backend.service;

import com.adoptify.backend.model.AdoptionRequest;
import com.adoptify.backend.model.Animal;
import com.adoptify.backend.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@adoptify.com}")
    private String fromEmail;

    @Async
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("Email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendHtmlMessage(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            logger.info("HTML email sent to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send HTML email to {}: {}", to, e.getMessage());
        }
    }

    public void sendAdoptionRequestEmailToOwner(AdoptionRequest request) {
        String to = request.getAnimal().getOwner().getEmail();
        String subject = "🐾 New Adoption Request for " + request.getAnimal().getName();
        
        String html = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eee; padding: 20px; border-radius: 10px;'>"
                + "<h2 style='color: #f59e0b;'>New Adoption Request!</h2>"
                + "<p>Hello <strong>" + request.getAnimal().getOwner().getFullName() + "</strong>,</p>"
                + "<p><strong>" + request.getAdopter().getFullName() + "</strong> is interested in adopting <strong>" + request.getAnimal().getName() + "</strong>.</p>"
                + "<h3>Request Details:</h3>"
                + "<ul>"
                + "<li><strong>Living Situation:</strong> " + request.getLivingSituation() + "</li>"
                + "<li><strong>Pet Experience:</strong> " + (request.getHasExperience() ? "Experienced" : "First-time owner") + "</li>"
                + "<li><strong>Reason:</strong> " + request.getReasonForAdoption() + "</li>"
                + "<li><strong>Preferred Meeting:</strong> " + request.getPreferredMeetingDate() + "</li>"
                + "</ul>"
                + "<div style='text-align: center; margin-top: 20px;'>"
                + "<a href='http://localhost:5501/frontend/dashboard.html' style='background-color: #f59e0b; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; font-weight: bold;'>View Request</a>"
                + "</div>"
                + "<hr><p style='color: #888; font-size: 12px;'>This is an automated message from Adoptify.</p></div>";
        
        sendHtmlMessage(to, subject, html);
    }

    public void sendAdoptionApprovedEmailToAdopter(AdoptionRequest request) {
        String to = request.getAdopter().getEmail();
        String subject = "✅ Adoption Request Approved for " + request.getAnimal().getName();
        User owner = request.getAnimal().getOwner();

        String html = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eee; padding: 20px; border-radius: 10px;'>"
                + "<h2 style='color: #10b981;'>Congratulations!</h2>"
                + "<p>Your adoption request for <strong>" + request.getAnimal().getName() + "</strong> has been approved by <strong>" + owner.getFullName() + "</strong>.</p>"
                + "<h3>Owner Contact Details:</h3>"
                + "<ul>"
                + "<li><strong>Phone:</strong> " + owner.getPhone() + "</li>"
                + "<li><strong>Email:</strong> " + owner.getEmail() + "</li>"
                + "<li><strong>Address:</strong> " + owner.getAddress() + "</li>"
                + "</ul>"
                + "<p>Please contact the owner to finalize the adoption process.</p>"
                + "<div style='text-align: center; margin-top: 20px;'>"
                + "<a href='http://localhost:5501/frontend/dashboard.html' style='background-color: #10b981; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; font-weight: bold;'>View Details</a>"
                + "</div>"
                + "<hr><p style='color: #888; font-size: 12px;'>This is an automated message from Adoptify.</p></div>";

        sendHtmlMessage(to, subject, html);
    }

    public void sendAdoptionRejectedEmailToAdopter(AdoptionRequest request) {
        String to = request.getAdopter().getEmail();
        String subject = "❌ Adoption Request Update for " + request.getAnimal().getName();

        String html = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eee; padding: 20px; border-radius: 10px;'>"
                + "<h2 style='color: #ef4444;'>Adoption Update</h2>"
                + "<p>We're sorry, your adoption request for <strong>" + request.getAnimal().getName() + "</strong> has not been approved at this time.</p>"
                + "<h3>Reason:</h3>"
                + "<p style='background: #fff5f5; padding: 10px; border-left: 4px solid #ef4444;'>" + request.getRejectionReason() + "</p>"
                + "<p>Don't lose hope—there are many other animals looking for a loving home!</p>"
                + "<div style='text-align: center; margin-top: 20px;'>"
                + "<a href='http://localhost:5501/frontend/index.html' style='background-color: #f59e0b; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Browse Other Animals</a>"
                + "</div>"
                + "<hr><p style='color: #888; font-size: 12px;'>This is an automated message from Adoptify.</p></div>";

        sendHtmlMessage(to, subject, html);
    }

    public void sendPasswordResetEmail(String to, String resetLink, String fullName) {
        String subject = "Adoptify - Reset Your Password";
        String html = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2>Password Reset Request</h2>"
                + "<p>Hello " + fullName + ",</p>"
                + "<p>You requested a password reset. Click the button below to set a new password:</p>"
                + "<div style='text-align: center; margin: 30px 0;'>"
                + "<a href='" + resetLink + "' style='background-color: #f59e0b; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Reset Password</a>"
                + "</div>"
                + "<p>If you didn't request this, you can safely ignore this email.</p>"
                + "<hr><p style='color: #888; font-size: 12px;'>This link will expire in 24 hours.</p></div>";

        sendHtmlMessage(to, subject, html);
    }

    public void sendRescueAlertEmail(String rescuerEmail, String rescuerName,
            String animalType, String animalCondition, String emergencyLevel,
            String location, String landmark, String reporterName, String reporterPhone, String description) {
        
        String subject = "🆘 URGENT: Animal Rescue Alert - " + animalType;

        String html = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 2px solid #ff4444; padding: 20px; border-radius: 10px;\">"
                + "<h2 style=\"color: #ff4444; text-align: center;\">🆘 URGENT RESCUE ALERT</h2>"
                + "<p>Hello <strong>" + rescuerName + "</strong>,</p>"
                + "<p>A new rescue request has been reported. Immediate attention is required.</p>"
                + "<div style=\"background-color: #fff4f4; padding: 15px; border-radius: 5px;\">"
                + "<h3>Rescue Details:</h3>"
                + "<ul>"
                + "<li><strong>Animal Type:</strong> " + animalType + "</li>"
                + "<li><strong>Condition:</strong> " + animalCondition + "</li>"
                + "<li><strong>Emergency Level:</strong> <span style=\"color: #ff4444; font-weight: bold;\">" + emergencyLevel + "</span></li>"
                + "<li><strong>Location:</strong> " + location + "</li>"
                + "<li><strong>Landmark:</strong> " + (landmark != null ? landmark : "N/A") + "</li>"
                + "</ul>"
                + "<h3>Reporter Information:</h3>"
                + "<ul>"
                + "<li><strong>Name:</strong> " + reporterName + "</li>"
                + "<li><strong>Phone:</strong> " + reporterPhone + "</li>"
                + "</ul>"
                + "<h3>Description:</h3>"
                + "<p style=\"font-style: italic; background: #fff; padding: 10px; border: 1px solid #ddd;\">" + description + "</p>"
                + "</div>"
                + "<div style=\"text-align: center; margin-top: 20px;\">"
                + "<a href=\"http://localhost:8080/rescue-alerts\" style=\"background-color: #ff4444; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;\">ACCEPT RESCUE NOW</a>"
                + "</div>"
                + "<p style=\"margin-top: 20px;\">Please log in to Adoptify to view more details and coordinate the rescue.</p>"
                + "<hr>"
                + "<p style=\"color: #888; font-size: 11px; text-align: center;\">This is an automated emergency message from Adoptify Rescue System.</p>"
                + "</div>";

        sendHtmlMessage(rescuerEmail, subject, html);
    }
}
