package com.example.parkio.dto.request;

import com.example.parkio.entity.ScanLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record OfflineScanBatchRequest(
        @NotEmpty List<@Valid Entry> scans
) {
    public record Entry(
            @NotBlank String qrCode,
            @NotNull ScanLog.ScanAction action,
            /** When the scan actually happened on the device, offline — NOT when it's being synced now. */
            @NotNull LocalDateTime scanTime,
            Double latitude,
            Double longitude
    ) {}
}
