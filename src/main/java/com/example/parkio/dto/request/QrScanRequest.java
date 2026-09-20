package com.example.parkio.dto.request;

import com.example.parkio.entity.ScanLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QrScanRequest(
        @NotBlank String qrCode,
        @NotNull ScanLog.ScanAction action,
        Double latitude,
        Double longitude
) {}
