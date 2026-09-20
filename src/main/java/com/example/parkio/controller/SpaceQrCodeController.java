package com.example.parkio.controller;

import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.ParkingLotResponse;
import com.example.parkio.dto.response.SpaceQrCodeResponse;
import com.example.parkio.service.SpaceQrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SpaceQrCodeController {

    private final SpaceQrCodeService spaceQrCodeService;

    /** Admin: generate (or fetch, if already generated) the permanent QR for a space. */
    @PostMapping("/api/v1/parking-lots/{id}/qr-code")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<SpaceQrCodeResponse>> generate(@PathVariable Long id) {
        // Consistent with this codebase's existing convention of a literal
        // "admin" actor label for admin-only mutations (see ParkingSpotService).
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(spaceQrCodeService.generate(id, "admin")));
    }

    /** Public — scanning the physical sign resolves to space info only, no auth, no booking side-effect. */
    @GetMapping("/api/v1/qr/{code}")
    public ResponseEntity<ApiResponse<ParkingLotResponse>> resolve(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(spaceQrCodeService.resolve(code)));
    }
}
