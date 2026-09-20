package com.example.parkio.controller;

import com.example.parkio.dto.request.PricingRuleRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.PricingRuleResponse;
import com.example.parkio.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    /** Public — see what rules apply to a lot */
    @GetMapping("/parking-lots/{lotId}/pricing-rules")
    public ResponseEntity<ApiResponse<List<PricingRuleResponse>>> getByLot(
            @PathVariable Long lotId) {
        return ResponseEntity.ok(ApiResponse.ok(pricingService.getByLot(lotId)));
    }

    @PostMapping("/parking-lots/{lotId}/pricing-rules")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PricingRuleResponse>> create(
            @PathVariable Long lotId,
            @Valid @RequestBody PricingRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(pricingService.create(lotId, request)));
    }

    @PutMapping("/pricing-rules/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PricingRuleResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PricingRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Rule updated", pricingService.update(id, request)));
    }

    @DeleteMapping("/pricing-rules/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        pricingService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Pricing rule deleted"));
    }
}
