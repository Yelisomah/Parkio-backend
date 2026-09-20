package com.example.parkio.service;

import com.example.parkio.dto.response.NotificationResponse;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.entity.Notification;
import com.example.parkio.entity.User;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    // ── Async creation (called from other services) ───────────────────────────

    @Async
    @Transactional
    public void send(Long userId, Notification.NotificationType type,
                     String title, String message,
                     Long referenceId, String referenceType) {
        User user = userService.findById(userId);
        Notification n = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
        notificationRepository.save(n);
    }

    /** Shorthand without reference. */
    @Async
    @Transactional
    public void send(Long userId, Notification.NotificationType type,
                     String title, String message) {
        send(userId, type, title, message, null, null);
    }

    // ── User-facing queries ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAll(Long userId, Pageable pageable) {
        return PageResponse.from(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                        .map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUnread(Long userId, Pageable pageable) {
        return PageResponse.from(
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                        .map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ParkioException.notFound("Notification not found: " + notificationId));
        if (!n.getUser().getId().equals(userId)) {
            throw ParkioException.forbidden("Access denied");
        }
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(java.time.LocalDateTime.now());
            notificationRepository.save(n);
        }
        return NotificationResponse.from(n);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    @Transactional
    public int deleteReadOlderThan(java.time.LocalDateTime cutoff) {
        return notificationRepository.deleteReadOlderThan(cutoff);
    }
}
