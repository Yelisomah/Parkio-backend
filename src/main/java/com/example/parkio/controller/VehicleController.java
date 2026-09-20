package com.example.parkio.controller;

import com.example.parkio.dto.request.VehicleRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.VehicleResponse;
import com.example.parkio.service.VehicleService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getMyVehicles() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getByOwner(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getById(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getById(id, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> create(
            @Valid @RequestBody VehicleRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        VehicleResponse response = vehicleService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Vehicle updated", vehicleService.update(id, userId, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        vehicleService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle deleted"));
    }
}
