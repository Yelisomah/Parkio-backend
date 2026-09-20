package com.example.parkio.service;

import com.example.parkio.dto.request.LoginRequest;
import com.example.parkio.dto.request.RefreshTokenRequest;
import com.example.parkio.dto.request.RegisterRequest;
import com.example.parkio.dto.response.AuthResponse;
import com.example.parkio.dto.response.UserResponse;
import com.example.parkio.entity.AuditLog;
import com.example.parkio.entity.Otp;
import com.example.parkio.entity.Role;
import com.example.parkio.entity.User;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.RoleRepository;
import com.example.parkio.repository.UserRepository;
import com.example.parkio.security.JwtUtil;
import com.example.parkio.security.TokenBlacklist;
import com.example.parkio.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository          userRepository;
        private final RoleRepository          roleRepository;
        private final PasswordEncoder         passwordEncoder;
        private final AuthenticationManager   authenticationManager;
        private final JwtUtil                 jwtUtil;
        private final UserDetailsServiceImpl  userDetailsService;
        private final TokenBlacklist          tokenBlacklist;
        private final AuditService            auditService;
        private final OtpService              otpService;


        // ── Register ──────────────────────────────────────────────────────────────

        @Transactional
        public AuthResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.email())) {
                        throw ParkioException.conflict("Email already in use: " + request.email());
                }

                if (request.phone() != null && !request.phone().isBlank()
                        && userRepository.existsByPhone(request.phone())) {
                        throw ParkioException.conflict("Phone number already in use: " + request.phone());
                }

                if (request.phone() != null && !request.phone().isBlank()) {
                        otpService.verifyOtp(request.phone(), request.otpCode(), Otp.Purpose.REGISTRATION);
                }

                Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                        .orElseThrow(() -> ParkioException.notFound("Default role not found. Run data seeder."));

                User user = User.builder()
                        .firstName(request.firstName())
                        .lastName(request.lastName())
                        .email(request.email())
                        .password(passwordEncoder.encode(request.password()))
                        .phone(request.phone())
                        .phoneVerified(request.phone() != null && !request.phone().isBlank())
                        .roles(Set.of(userRole))
                        .build();

                User saved = userRepository.save(user);

                String identifier = saved.getEmail() != null ? saved.getEmail() : saved.getPhone();
                auditService.log(identifier, AuditLog.AuditAction.USER_REGISTERED,
                        "User", saved.getId(), "New user registered: " + identifier);

                UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);
                String accessToken  = jwtUtil.generateAccessToken(userDetails);
                String refreshToken = jwtUtil.generateRefreshToken(userDetails);

                return new AuthResponse(accessToken, refreshToken, jwtUtil.getExpirationMillis(),
                        UserResponse.from(saved));
        }

        public String requestOtp(String phone) {
                // In dev-mode, we return the OTP directly instead of sending an SMS
                return otpService.requestOtp(phone, Otp.Purpose.REGISTRATION);
        }

        // ── Login ─────────────────────────────────────────────────────────────────

        public AuthResponse login(LoginRequest request) {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(request.email(), request.password()));

                User user = userRepository.findByEmailOrPhoneWithRoles(request.email())
                        .orElseThrow(() -> ParkioException.notFound("User not found"));

                auditService.log(request.email(), AuditLog.AuditAction.USER_LOGIN,
                        "User", user.getId(), "User logged in");

                UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
                String accessToken  = jwtUtil.generateAccessToken(userDetails);
                String refreshToken = jwtUtil.generateRefreshToken(userDetails);

                return new AuthResponse(accessToken, refreshToken, jwtUtil.getExpirationMillis(),
                        UserResponse.from(user));
        }

        // ── Refresh ───────────────────────────────────────────────────────────────

        public AuthResponse refreshToken(RefreshTokenRequest request) {
                String token = request.refreshToken();

                if (jwtUtil.isTokenExpired(token)) {
                throw ParkioException.badRequest("Refresh token has expired");
                }

                String identifier = jwtUtil.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);

                if (!jwtUtil.isTokenValid(token, userDetails)) {
                throw ParkioException.badRequest("Invalid refresh token");
                }

                User user = userRepository.findByEmailOrPhoneWithRoles(identifier)
                        .orElseThrow(() -> ParkioException.notFound("User not found"));

                String newAccessToken  = jwtUtil.generateAccessToken(userDetails);
                String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

                return new AuthResponse(newAccessToken, newRefreshToken, jwtUtil.getExpirationMillis(),
                        UserResponse.from(user));
        }

        // ── Logout ────────────────────────────────────────────────────────────────

        public void logout(String accessToken) {
                if (accessToken == null || accessToken.isBlank()) return;
                try {
                    String email = jwtUtil.extractUsername(accessToken);
                java.util.Date expiry = jwtUtil.extractExpiration(accessToken);
                tokenBlacklist.revoke(accessToken, expiry);

                    userRepository.findByEmailOrPhone(email, email).ifPresent(user ->
                        auditService.log(email, AuditLog.AuditAction.USER_LOGOUT,
                                "User", user.getId(), "User logged out"));
                } catch (Exception ignored) {
                // Token already invalid — logout is a no-op
                }
        }
}
