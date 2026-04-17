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

    @Value("${app.frontend.url:http://localhost:5501/frontend}")
    private String frontendUrl;

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
        
        String html = "<div style='font-family: \"Inter\", sans-serif; max-width: 600px; margin: 20px auto; border: 1px solid #e2e8f0; padding: 40px; border-radius: 16px; background-color: #ffffff; color: #1a202c;'>"
                + "<div style='text-align: center; margin-bottom: 30px;'><h1 style='color: #f59e0b; margin: 0; font-size: 28px;'>🐾 New Adoption Request!</h1></div>"
                + "<p style='font-size: 16px; line-height: 1.6;'>Hello <strong>" + request.getAnimal().getOwner().getFullName() + "</strong>,</p>"
                + "<p style='font-size: 16px; line-height: 1.6;'>Great news! <strong>" + request.getAdopter().getFullName() + "</strong> is interested in adopting <strong>" + request.getAnimal().getName() + "</strong>. Here's a brief summary of their application:</p>"
                + "<div style='background-color: #f8fafc; padding: 25px; border-radius: 12px; margin: 30px 0;'>"
                + "<h3 style='margin-top: 0; color: #64748b; font-size: 14px; text-transform: uppercase; letter-spacing: 0.05em;'>Request Details</h3>"
                + "<table style='width: 100%; border-collapse: collapse;'>"
                + "<tr><td style='padding: 8px 0; color: #475569; font-weight: 600;'>Living Situation:</td><td style='padding: 8px 0;'>" + request.getLivingSituation() + "</td></tr>"
                + "<tr><td style='padding: 8px 0; color: #475569; font-weight: 600;'>Pet Experience:</td><td style='padding: 8px 0;'>" + (request.getHasExperience() ? "Experienced" : "First-time owner") + "</td></tr>"
                + "<tr><td style='padding: 8px 0; color: #475569; font-weight: 600;'>Preferred Meeting:</td><td style='padding: 8px 0;'>" + request.getPreferredMeetingDate() + "</td></tr>"
                + "</table>"
                + "<h3 style='margin-top: 20px; color: #64748b; font-size: 14px; text-transform: uppercase; letter-spacing: 0.05em;'>Reason</h3>"
                + "<p style='margin: 0; color: #1e293b; font-style: italic; line-height: 1.5;'>\"" + request.getReasonForAdoption() + "\"</p>"
                + "</div>"
                + "<div style='text-align: center; margin-top: 40px;'>"
                + "<a href='" + frontendUrl + "/animals/received-requests.html' style='background-color: #f59e0b; color: white; padding: 16px 32px; text-decoration: none; border-radius: 12px; font-weight: bold; font-size: 16px; display: inline-block; box-shadow: 0 4px 6px -1px rgba(245, 158, 11, 0.4);'>Review Request</a>"
                + "</div>"
                + "<div style='margin-top: 40px; padding-top: 20px; border-top: 1px solid #e2e8f0; text-align: center; color: #94a3b8; font-size: 12px;'>"
                + "<p>Adoptify - Helping Hands for Paws</p>"
                + "</div></div>";
        
        sendHtmlMessage(to, subject, html);
    }

    public void sendAdoptionApprovedEmailToAdopter(AdoptionRequest request) {
        String to = request.getAdopter().getEmail();
        String subject = "✅ Adoption Request Approved for " + request.getAnimal().getName();
        User owner = request.getAnimal().getOwner();

        String html = "<div style='font-family: \"Inter\", sans-serif; max-width: 600px; margin: 20px auto; border: 1px solid #e2e8f0; padding: 40px; border-radius: 16px; background-color: #ffffff; color: #1a202c;'>"
                + "<div style='text-align: center; margin-bottom: 30px;'><h1 style='color: #10b981; margin: 0; font-size: 28px;'>🎉 Congratulations!</h1></div>"
                + "<p style='font-size: 16px; line-height: 1.6;'>Hello <strong>" + request.getAdopter().getFullName() + "</strong>,</p>"
                + "<p style='font-size: 16px; line-height: 1.6;'>Your adoption request for <strong>" + request.getAnimal().getName() + "</strong> has been approved by <strong>" + owner.getFullName() + "</strong>! You're one step closer to meeting your new best friend.</p>"
                + "<div style='background-color: #f0fdf4; padding: 25px; border-radius: 12px; margin: 30px 0; border: 1px solid #bbf7d0;'>"
                + "<h3 style='margin-top: 0; color: #166534; font-size: 14px; text-transform: uppercase; letter-spacing: 0.05em;'>Owner Contact Details</h3>"
                + "<table style='width: 100%; border-collapse: collapse;'>"
                + "<tr><td style='padding: 8px 0; color: #15803d; font-weight: 600;'>Phone:</td><td style='padding: 8px 0;'>" + owner.getPhone() + "</td></tr>"
                + "<tr><td style='padding: 8px 0; color: #15803d; font-weight: 600;'>Email:</td><td style='padding: 8px 0;'>" + owner.getEmail() + "</td></tr>"
                + "<tr><td style='padding: 8px 0; color: #15803d; font-weight: 600;'>Location:</td><td style='padding: 8px 0;'>" + owner.getAddress() + "</td></tr>"
                + "</table>"
                + "</div>"
                + "<p style='font-size: 14px; color: #64748b; text-align: center;'>Please contact the owner directly to arrange a meeting and finalize the adoption.</p>"
                + "<div style='text-align: center; margin-top: 40px;'>"
                + "<a href='" + frontendUrl + "/animals/sent-requests.html' style='background-color: #10b981; color: white; padding: 16px 32px; text-decoration: none; border-radius: 12px; font-weight: bold; font-size: 16px; display: inline-block; box-shadow: 0 4px 6px -1px rgba(16, 185, 129, 0.4);'>View My Applications</a>"
                + "</div>"
                + "</div>";

        sendHtmlMessage(to, subject, html);
    }

    public void sendAdoptionRejectedEmailToAdopter(AdoptionRequest request) {
        String to = request.getAdopter().getEmail();
        String subject = "❌ Adoption Request Update for " + request.getAnimal().getName();

        String html = "<div style='font-family: \"Inter\", sans-serif; max-width: 600px; margin: 20px auto; border: 1px solid #e2e8f0; padding: 40px; border-radius: 16px; background-color: #ffffff; color: #1a202c;'>"
                + "<div style='text-align: center; margin-bottom: 30px;'><h1 style='color: #ef4444; margin: 0; font-size: 28px;'>Adoption Update</h1></div>"
                + "<p style='font-size: 16px; line-height: 1.6;'>Hello <strong>" + request.getAdopter().getFullName() + "</strong>,</p>"
                + "<p style='font-size: 16px; line-height: 1.6;'>We're sorry to inform you that your adoption request for <strong>" + request.getAnimal().getName() + "</strong> has not been approved at this time.</p>"
                + "<div style='background-color: #fef2f2; padding: 25px; border-radius: 12px; margin: 30px 0; border: 1px solid #fee2e2;'>"
                + "<h3 style='margin-top: 0; color: #991b1b; font-size: 14px; text-transform: uppercase; letter-spacing: 0.05em;'>Reason from Owner</h3>"
                + "<p style='margin: 0; color: #1e293b; line-height: 1.5;'>\"" + request.getRejectionReason() + "\"</p>"
                + "</div>"
                + "<p style='font-size: 14px; color: #64748b;'>Don't lose hope—there are many other animals looking for a loving home! Keep browsing to find your perfect match.</p>"
                + "<div style='text-align: center; margin-top: 40px;'>"
                + "<a href='" + frontendUrl + "/animals/browse.html' style='background-color: #f59e0b; color: white; padding: 16px 32px; text-decoration: none; border-radius: 12px; font-weight: bold; font-size: 16px; display: inline-block; box-shadow: 0 4px 6px -1px rgba(245, 158, 11, 0.4);'>Browse Other Animals</a>"
                + "</div>"
                + "</div>";

        sendHtmlMessage(to, subject, html);
    }

    public void sendPasswordResetEmail(String to, String resetLink, String fullName) {
        String subject = "Adoptify - Reset Your Password";
        String html = "<div style='font-family: \"Inter\", sans-serif; max-width: 600px; margin: 20px auto; border: 1px solid #e2e8f0; padding: 40px; border-radius: 16px; background-color: #ffffff; color: #1a202c; text-align: center;'>"
                + "<h2 style='color: #1a202c; margin-bottom: 20px;'>Password Reset Request</h2>"
                + "<p style='font-size: 16px; line-height: 1.6; color: #4a5568;'>Hello <strong>" + fullName + "</strong>,</p>"
                + "<p style='font-size: 16px; line-height: 1.6; color: #4a5568;'>We received a request to reset your password. Click the button below to secure your account:</p>"
                + "<div style='margin: 40px 0;'>"
                + "<a href='" + resetLink + "' style='background-color: #f59e0b; color: white; padding: 18px 36px; text-decoration: none; border-radius: 12px; font-weight: 800; font-size: 16px; display: inline-block; box-shadow: 0 4px 6px -1px rgba(245, 158, 11, 0.4);'>RESET PASSWORD</a>"
                + "</div>"
                + "<p style='font-size: 13px; color: #718096;'>If you didn't request this, you can safely ignore this email. This link will expire in 24 hours.</p>"
                + "</div>";

        sendHtmlMessage(to, subject, html);
    }

    public void sendRescueAlertEmail(String rescuerEmail, String rescuerName,
            String animalType, String animalCondition, String emergencyLevel,
            String location, String landmark, String reporterName, String reporterPhone, String description) {
        
        String subject = "🆘 URGENT: Animal Rescue Alert - " + animalType;

        String html = "<div style='font-family: \"Inter\", sans-serif; max-width: 600px; margin: 20px auto; border: 2px solid #ef4444; padding: 40px; border-radius: 20px; background-color: #ffffff;'>"
                + "<div style='text-align: center; margin-bottom: 30px;'><h1 style='color: #ef4444; margin: 0; font-size: 28px;'>🆘 URGENT RESCUE ALERT</h1></div>"
                + "<p style='font-size: 16px;'>Hello <strong>" + rescuerName + "</strong>,</p>"
                + "<p style='font-size: 16px;'>A new rescue request has been reported near you. Immediate attention is required.</p>"
                + "<div style='background-color: #fef2f2; padding: 25px; border-radius: 16px; margin: 30px 0; border: 1px solid #fee2e2;'>"
                + "<h3 style='margin-top: 0; color: #991b1b; font-size: 14px; text-transform: uppercase; letter-spacing: 0.05em;'>Mission Intel</h3>"
                + "<table style='width: 100%; border-collapse: collapse;'>"
                + "<tr><td style='padding: 8px 0; color: #b91c1c; font-weight: 600;'>Animal Type:</td><td style='padding: 8px 0;'>" + animalType + "</td></tr>"
                + "<tr><td style='padding: 8px 0; color: #b91c1c; font-weight: 600;'>Condition:</td><td style='padding: 8px 0;'>" + animalCondition + "</td></tr>"
                + "<tr><td style='padding: 8px 0; color: #b91c1c; font-weight: 600;'>Emergency:</td><td style='padding: 8px 0; color: #ef4444; font-weight: 800;'>" + emergencyLevel + "</td></tr>"
                + "<tr><td style='padding: 8px 0; color: #b91c1c; font-weight: 600;'>Location:</td><td style='padding: 8px 0;'>" + location + "</td></tr>"
                + "<tr><td style='padding: 8px 0; color: #b91c1c; font-weight: 600;'>Landmark:</td><td style='padding: 8px 0;'>" + (landmark != null ? landmark : "N/A") + "</td></tr>"
                + "</table>"
                + "</div>"
                + "<div style='background-color: #f8fafc; padding: 15px; border-radius: 10px; margin-bottom: 30px;'>"
                + "<h4 style='margin: 0 0 10px 0; font-size: 13px; color: #64748b;'>REPORTER DETAILS</h4>"
                + "<p style='margin: 0; font-size: 14px;'><strong>" + reporterName + "</strong>: " + reporterPhone + "</p>"
                + "</div>"
                + "<div style='text-align: center; margin-top: 40px;'>"
                + "<a href='" + frontendUrl + "/ngo/ngo-dashboard.html' style='background-color: #ef4444; color: white; padding: 18px 36px; text-decoration: none; border-radius: 14px; font-weight: 900; font-size: 18px; display: inline-block; box-shadow: 0 10px 15px -3px rgba(239, 68, 68, 0.4);'>ACCEPT RESCUE NOW</a>"
                + "</div>"
                + "</div>";

        sendHtmlMessage(rescuerEmail, subject, html);
    }

    public void sendNGOVerificationEmail(User ngo) {
        String to = ngo.getEmail();
        String subject = "🎊 Welcome to the Adoptify Network - NGO Verified!";
        
        String html = "<div style='font-family: \"Inter\", sans-serif; max-width: 600px; margin: 20px auto; border: 1px solid #e2e8f0; padding: 40px; border-radius: 16px; background-color: #ffffff; color: #1a202c;'>"
                + "<div style='text-align: center; margin-bottom: 30px;'><h1 style='color: #f59e0b; margin: 0; font-size: 28px;'>🎊 Congratulations!</h1></div>"
                + "<p style='font-size: 16px; line-height: 1.6;'>Hello <strong>" + ngo.getOrganizationName() + "</strong>,</p>"
                + "<p style='font-size: 16px; line-height: 1.6;'>We are pleased to inform you that your NGO account has been **verified** by our administrative team. You are now officially part of our rescue network.</p>"
                + "<div style='background-color: #f0fdf4; padding: 25px; border-radius: 12px; margin: 30px 0; border: 1px solid #bbf7d0;'>"
                + "<h3 style='margin-top: 0; color: #166534; font-size: 14px; text-transform: uppercase; letter-spacing: 0.05em;'>Account Privileges</h3>"
                + "<ul style='color: #15803d; padding-left: 20px;'>"
                + "<li>Receive Real-time Rescue Alerts</li>"
                + "<li>Accept & Manage Rescue Missions</li>"
                + "<li>Official Verification Badge on Profile</li>"
                + "</ul>"
                + "</div>"
                + "<div style='text-align: center; margin-top: 40px;'>"
                + "<a href='" + frontendUrl + "/ngo/ngo-dashboard.html' style='background-color: #f59e0b; color: white; padding: 16px 32px; text-decoration: none; border-radius: 12px; font-weight: bold; font-size: 16px; display: inline-block;'>Access NGO Dashboard</a>"
                + "</div>"
                + "</div>";

        sendHtmlMessage(to, subject, html);
    }

    public void sendNGORejectionEmail(User ngo, String reason) {
        String to = ngo.getEmail();
        String subject = "Update regarding your NGO Registration - Adoptify";
        
        String html = "<div style='font-family: \"Inter\", sans-serif; max-width: 600px; margin: 20px auto; border: 1px solid #e2e8f0; padding: 40px; border-radius: 16px; background-color: #ffffff; color: #1a202c;'>"
                + "<div style='text-align: center; margin-bottom: 30px;'><h1 style='color: #ef4444; margin: 0; font-size: 28px;'>Registration Update</h1></div>"
                + "<p style='font-size: 16px; line-height: 1.6;'>Hello <strong>" + ngo.getOrganizationName() + "</strong>,</p>"
                + "<p style='font-size: 16px; line-height: 1.6;'>Thank you for your interest in joining Adoptify. After reviewing your application, we regret to inform you that we cannot approve your NGO registration at this time.</p>"
                + "<div style='background-color: #fef2f2; padding: 25px; border-radius: 12px; margin: 30px 0; border: 1px solid #fee2e2;'>"
                + "<h3 style='margin-top: 0; color: #991b1b; font-size: 14px; text-transform: uppercase; letter-spacing: 0.05em;'>Reason from Admin</h3>"
                + "<p style='margin: 0; color: #1e293b; line-height: 1.5;'>\"" + reason + "\"</p>"
                + "</div>"
                + "<p style='font-size: 14px; color: #64748b;'>If you believe this was an error or would like to re-apply with corrected information, please contact our support team.</p>"
                + "</div>";

        sendHtmlMessage(to, subject, html);
    }

    public void sendRescueAcceptedEmailToReporter(com.adoptify.backend.model.RescueReport report, User ngo, java.time.LocalDateTime eta) {
        if (report.getReporter() == null) return;
        
        String to = report.getReporter().getEmail();
        String subject = "🙏 Help is on the way! Rescue Accepted for " + report.getAnimalType();
        
        String timeStr = eta.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
        
        String html = "<div style='font-family: \"Inter\", sans-serif; max-width: 600px; margin: 20px auto; border: 1px solid #3b82f6; padding: 40px; border-radius: 20px; background-color: #ffffff;'>"
                + "<div style='text-align: center; margin-bottom: 30px;'><h1 style='color: #3b82f6; margin: 0; font-size: 28px;'>🙏 Hero is coming!</h1></div>"
                + "<p style='font-size: 16px;'>Hello <strong>" + report.getReporterName() + "</strong>,</p>"
                + "<p style='font-size: 16px;'>Great news! <strong>" + ngo.getOrganizationName() + "</strong> has accepted your rescue request for the " + report.getAnimalType() + ".</p>"
                + "<div style='background-color: #eff6ff; padding: 25px; border-radius: 16px; margin: 30px 0; border: 1px solid #bfdbfe;'>"
                + "<h3 style='margin-top: 0; color: #1e40af; font-size: 14px; text-transform: uppercase; letter-spacing: 0.05em;'>Responder Details</h3>"
                + "<table style='width: 100%; border-collapse: collapse;'>"
                + "<tr><td style='padding: 8px 0; color: #2563eb; font-weight: 600;'>NGO:</td><td style='padding: 8px 0;'>" + ngo.getOrganizationName() + "</td></tr>"
                + "<tr><td style='padding: 8px 0; color: #2563eb; font-weight: 600;'>Contact:</td><td style='padding: 8px 0;'>" + ngo.getPhone() + "</td></tr>"
                + "<tr><td style='padding: 8px 0; color: #2563eb; font-weight: 600;'>Estimated Arrival:</td><td style='padding: 8px 0; font-weight: 800; color: #1e3a8a;'>" + timeStr + "</td></tr>"
                + "</table>"
                + "</div>"
                + "<p style='font-size: 14px; color: #64748b;'>You can track the live status of the rescue on our website.</p>"
                + "<div style='text-align: center; margin-top: 40px;'>"
                + "<a href='" + frontendUrl + "/rescue/rescue.html' style='background-color: #3b82f6; color: white; padding: 18px 36px; text-decoration: none; border-radius: 14px; font-weight: 900; font-size: 18px; display: inline-block; box-shadow: 0 10px 15px -3px rgba(59, 130, 246, 0.4);'>Track Rescue Status</a>"
                + "</div>"
                + "</div>";
        
        sendHtmlMessage(to, subject, html);
    }
    public void sendRescueCompletedEmailToReporter(com.adoptify.backend.model.RescueReport report, User ngo) {
        if (report.getReporter() == null) return;

        String to = report.getReporter().getEmail();
        String subject = "✨ Mission Accomplished: " + report.getAnimalType() + " Rescued!";

        String html = "<div style='font-family: \"Inter\", sans-serif; max-width: 600px; margin: 20px auto; border: 1px solid #10b981; padding: 40px; border-radius: 20px; background-color: #ffffff; text-align: center;'>"
                + "<div style='margin-bottom: 30px;'><h1 style='color: #10b981; margin: 0; font-size: 28px;'>✨ Success!</h1></div>"
                + "<p style='font-size: 16px; text-align: left;'>Hello <strong>" + report.getReporterName() + "</strong>,</p>"
                + "<p style='font-size: 16px; text-align: left;'>We are happy to share that the rescue mission for the " + report.getAnimalType() + " has been **Completed** by <strong>" + ngo.getOrganizationName() + "</strong>.</p>"
                + "<div style='background-color: #f0fdf4; padding: 25px; border-radius: 16px; margin: 30px 0; border: 1px solid #bbf7d0; text-align: left;'>"
                + "<p style='margin: 0; color: #166534; font-weight: 500;'>Thank you for your quick action and reporting this incident. Your compassion helped save a life today!</p>"
                + "</div>"
                + "<p style='font-size: 14px; color: #64748b;'>Would you like to rate the service provided by the NGO? Your feedback helps us maintain high standards.</p>"
                + "<div style='margin-top: 40px;'>"
                + "<a href='" + frontendUrl + "/index.html' style='background-color: #10b981; color: white; padding: 18px 36px; text-decoration: none; border-radius: 14px; font-weight: 900; font-size: 18px; display: inline-block; box-shadow: 0 10px 15px -3px rgba(16, 185, 129, 0.4);'>Share Feedback</a>"
                + "</div>"
                + "</div>";

        sendHtmlMessage(to, subject, html);
    }
}
