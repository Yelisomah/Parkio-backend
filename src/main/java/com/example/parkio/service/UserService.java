package com.example.parkio.service;

import com.example.parkio.dto.request.ChangePasswordRequest;
import com.example.parkio.dto.request.UpdateUserRequest;
import com.example.parkio.dto.response.UserResponse;
import com.example.parkio.entity.AuditLog;
import com.example.parkio.entity.Notification;
import com.example.parkio.entity.User;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final AuditService      auditService;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       @Lazy NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return UserResponse.from(userRepository.findByEmail(email)
                .orElseThrow(() -> ParkioException.notFound("User not found: " + email)));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponse::from);
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findById(id);
        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName()  != null) user.setLastName(request.lastName());
        if (request.phone()     != null) user.setPhone(request.phone());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = findById(id);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ParkioException.badRequest("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        auditService.log(user.getEmail(), AuditLog.AuditAction.PASSWORD_CHANGED,
                "User", user.getId(), "Password changed by user");
    }

    @Transactional
    public void toggleEnabled(Long id, boolean enabled, String actorEmail) {
        User user = findById(id);
        user.setEnabled(enabled);
        userRepository.save(user);

        AuditLog.AuditAction action = enabled
                ? AuditLog.AuditAction.USER_ENABLED
                : AuditLog.AuditAction.USER_DISABLED;
        auditService.log(actorEmail, action, "User", id,
                (enabled ? "Enabled" : "Disabled") + " user: " + user.getEmail());

        if (!enabled) {
            notificationService.send(id, Notification.NotificationType.ACCOUNT_DISABLED,
                    "Account Disabled",
                    "Your Parkio account has been disabled. Contact support if you think this is a mistake.");
        }
    }

    @Transactional
    public void delete(Long id, String actorEmail) {
        User user = findById(id);
        String email = user.getEmail();
        userRepository.deleteById(id);

        auditService.log(actorEmail, AuditLog.AuditAction.USER_DELETED,
                "User", id, "Deleted user: " + email);
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ParkioException.notFound("User not found: " + id));
    }

    @Transactional
    public User findOrCreateGuestByPhone(String phone, String name) {
        return userRepository.findFirstByPhone(phone).orElseGet(() -> {
            User guest = User.builder()
                    .firstName(name == null || name.isBlank() ? "Guest" : name)
                    .lastName("")
                    .email("guest-" + java.util.UUID.randomUUID() + "@parkio.local")
                    .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .phone(phone)
                    .phoneVerified(true)
                    .build();
            return userRepository.save(guest);
        });
    }
}
