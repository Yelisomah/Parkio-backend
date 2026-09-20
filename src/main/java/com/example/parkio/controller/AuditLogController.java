package com.example.parkio.controller;

import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.AuditLogResponse;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAll(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(auditService.getAll(pageable))));
    }

    @GetMapping("/actor/{email}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getByActor(
            @PathVariable String email,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(auditService.getByActor(email, pageable))));
    }

    @GetMapping("/entity/{type}/{id}")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getByEntity(
            @PathVariable String type,
            @PathVariable Long id,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(auditService.getByEntity(type, id, pageable))));
    }
}
