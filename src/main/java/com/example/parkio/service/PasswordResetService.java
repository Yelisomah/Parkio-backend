package com.example.parkio.service;

import com.example.parkio.dto.request.ForgotPasswordRequest;
import com.example.parkio.dto.request.ResetPasswordRequest;
import com.example.parkio.entity.PasswordResetToken;
import com.example.parkio.entity.User;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.PasswordResetTokenRepository;
import com.example.parkio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@parkio.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private static final long TOKEN_VALIDITY_MINUTES = 30;

    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        // Always return 200 to prevent email enumeration
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            // Invalidate any existing tokens
            tokenRepository.deleteByUserId(user.getId());

            String rawToken = UUID.randomUUID().toString();
            PasswordResetToken token = PasswordResetToken.builder()
                    .token(rawToken)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES))
                    .build();
            tokenRepository.save(token);

            sendResetEmail(user, rawToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByTokenAndUsedFalse(request.token())
                .orElseThrow(() -> ParkioException.badRequest("Invalid or already used reset token"));

        if (token.isExpired()) {
            throw ParkioException.badRequest("Reset token has expired. Please request a new one.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    @Transactional
    public int cleanupExpiredTokens() {
        return tokenRepository.deleteExpired(LocalDateTime.now());
    }

    private void sendResetEmail(User user, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + token;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Parkio — Reset Your Password");
            message.setText(
                    "Hello " + user.getFirstName() + ",\n\n" +
                    "We received a request to reset your Parkio password.\n\n" +
                    "Click the link below to reset it (expires in " + TOKEN_VALIDITY_MINUTES + " minutes):\n" +
                    resetLink + "\n\n" +
                    "If you did not request this, please ignore this email.\n\n" +
                    "— The Parkio Team"
            );
            mailSender.send(message);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception ex) {
            // Log but don't fail — the token is already saved
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), ex.getMessage());
        }
    }
}
