package com.example.parkio.controller;

import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.NotificationResponse;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.service.NotificationService;
import com.example.parkio.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getAll(userId, pageable)));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getUnread(
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getUnread(userId, pageable)));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countUnread() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", notificationService.countUnread(userId))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", notificationService.markRead(id, userId)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        Long userId = securityUtils.getCurrentUserId();
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }
}
