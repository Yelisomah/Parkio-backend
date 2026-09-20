package com.example.parkio.service;

import com.example.parkio.dto.response.ParkingLotResponse;
import com.example.parkio.dto.response.SpaceQrCodeResponse;
import com.example.parkio.entity.AuditLog;
import com.example.parkio.entity.ParkingLot;
import com.example.parkio.entity.SpaceQrCode;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.SpaceQrCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpaceQrCodeService {

    private final SpaceQrCodeRepository spaceQrCodeRepository;
    private final ParkingLotService parkingLotService;
    private final AuditService auditService;

    /** Admin: generate the permanent QR for a space. Idempotent — returns the existing one if already generated. */
    @Transactional
    public SpaceQrCodeResponse generate(Long spaceId, String actorEmail) {
        return spaceQrCodeRepository.findBySpaceId(spaceId)
                .map(SpaceQrCodeResponse::from)
                .orElseGet(() -> {
                    ParkingLot space = parkingLotService.findById(spaceId);
                    SpaceQrCode qr = SpaceQrCode.builder()
                            .space(space)
                            .code("SPACE-" + UUID.randomUUID().toString().replace("-", "").toUpperCase())
                            .build();
                    SpaceQrCode saved = spaceQrCodeRepository.save(qr);

                    auditService.log(actorEmail, AuditLog.AuditAction.SPACE_QR_GENERATED,
                            "ParkingLot", space.getId(), "QR code generated for space " + space.getName());

                    return SpaceQrCodeResponse.from(saved);
                });
    }

    /** Public resolution endpoint — scanning the sign just shows space info, does NOT start a booking (that's Phase 2's guest-walkup flow). */
    @Transactional(readOnly = true)
    public ParkingLotResponse resolve(String code) {
        SpaceQrCode qr = spaceQrCodeRepository.findByCode(code)
                .orElseThrow(() -> ParkioException.notFound("Unrecognized QR code"));
        return parkingLotService.getById(qr.getSpace().getId());
    }
}
