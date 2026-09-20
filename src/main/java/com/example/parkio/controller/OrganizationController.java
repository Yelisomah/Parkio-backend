package com.example.parkio.controller;

import com.example.parkio.dto.request.OrganizationRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.OrganizationResponse;
import com.example.parkio.service.OrganizationService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final SecurityUtils securityUtils;

    /** Any authenticated user can create an organization — they become its first ORG_ADMIN. */
    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(@Valid @RequestBody OrganizationRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(organizationService.create(userId, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getById(id)));
    }

    /** Organizations owned by the current user. */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getMine() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(organizationService.getMine(userId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> update(
            @PathVariable Long id, @Valid @RequestBody OrganizationRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(organizationService.update(id, userId, request)));
    }
}
