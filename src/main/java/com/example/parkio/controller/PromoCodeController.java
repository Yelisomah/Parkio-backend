package com.example.parkio.controller;

import com.example.parkio.dto.request.PromoCodeRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.PromoCodeResponse;
import com.example.parkio.service.PromoCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/promo-codes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPS')")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @PostMapping
    public ResponseEntity<ApiResponse<PromoCodeResponse>> create(@Valid @RequestBody PromoCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(promoCodeService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromoCodeResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(promoCodeService.listAll()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        promoCodeService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Promo code deactivated"));
    }
}
