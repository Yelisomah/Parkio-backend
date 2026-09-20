package com.example.parkio.dto.response;

import com.example.parkio.entity.SpaceQrCode;

import java.time.LocalDateTime;

public record SpaceQrCodeResponse(
        String code,
        Long spaceId,
        String spaceName,
        LocalDateTime createdAt
) {
    public static SpaceQrCodeResponse from(SpaceQrCode qr) {
        return new SpaceQrCodeResponse(qr.getCode(), qr.getSpace().getId(), qr.getSpace().getName(), qr.getCreatedAt());
    }
}
