package com.university.ojt.service;

import com.resend.*;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${app.resend.api-key}")
    private String apiKey;

    @Value("${app.mail.from}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        Resend resend = new Resend(apiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
            .from(fromEmail)
            .to(toEmail)
            .subject("Password Reset Request - OJT System")
            .html("<p>You requested a password reset. Click the link below to reset your password:</p>" +
                  "<a href='" + resetLink + "'>Reset Password</a><br><br>" +
                  "<p>If the button doesn't work, copy and paste this link into your browser:</p>" +
                  "<p>" + resetLink + "</p>")
            .build();

        try {
            CreateEmailResponse data = resend.emails().send(params);
            System.out.println("Resend API Success! ID: " + data.getId());
        } catch (Exception e) {
            System.err.println("Resend API Error: " + e.getMessage());
        }
    }
}
