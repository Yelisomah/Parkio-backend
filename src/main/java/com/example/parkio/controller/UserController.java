package com.example.parkio.controller;

import com.example.parkio.dto.request.ChangePasswordRequest;
import com.example.parkio.dto.request.UpdateUserRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.dto.response.UserResponse;
import com.example.parkio.service.UserService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    /** Get own profile */
    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(userId)));
    }

    /** Update own profile */
    @PutMapping("/users/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @Valid @RequestBody UpdateUserRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", userService.update(userId, request)));
    }

    /** Change own password */
    @PatchMapping("/users/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(userService.getAll(pageable))));
    }

    @GetMapping("/admin/users/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }

    @PatchMapping("/admin/users/{id}/disable")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable Long id) {
        userService.toggleEnabled(id, false, securityUtils.getCurrentEmail());
        return ResponseEntity.ok(ApiResponse.ok("User disabled"));
    }

    @PatchMapping("/admin/users/{id}/enable")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> enableUser(@PathVariable Long id) {
        userService.toggleEnabled(id, true, securityUtils.getCurrentEmail());
        return ResponseEntity.ok(ApiResponse.ok("User enabled"));
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.delete(id, securityUtils.getCurrentEmail());
        return ResponseEntity.ok(ApiResponse.ok("User deleted"));
    }
}
