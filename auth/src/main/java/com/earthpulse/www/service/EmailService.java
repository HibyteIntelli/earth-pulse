package com.earthpulse.www.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.from}")
    private String from;

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Reset your EarthPulse password");
        message.setText("""
                You requested a password reset for your EarthPulse account.

                Click the link below to set a new password. It expires in 1 hour.

                %s/forgot-password?token=%s

                If you did not request this, you can safely ignore this email.
                """.formatted(frontendUrl, token));
        mailSender.send(message);
    }
}
