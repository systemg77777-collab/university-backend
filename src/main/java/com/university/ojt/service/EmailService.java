package com.university.ojt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendPasswordResetEmail(String to, String resetLink) {
        if (mailSender == null) {
            System.err.println("WARNING: JavaMailSender is not configured. Email not sent.");
            System.err.println("To: " + to);
            System.err.println("Reset Link: " + resetLink);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("UNI-Versity OJT System - Password Reset Request");
        message.setText("Hello,\n\n" +
                "You requested to reset your password for the UNI-Versity OJT System.\n" +
                "Click the link below to set a new password:\n\n" +
                resetLink + "\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Thank you,\nUNI-Versity Admin Team");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error sending email to " + to + ": " + e.getMessage());
            throw new RuntimeException("Failed to send email. Please check SMTP configuration.");
        }
    }
}
