package com.example.parkio.service;

import com.example.parkio.dto.request.ParkingSpotRequest;
import com.example.parkio.dto.request.BulkSpotRequest;
import com.example.parkio.dto.response.ParkingSpotResponse;
import com.example.parkio.entity.ParkingLot;
import com.example.parkio.entity.ParkingSpot;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingLotService parkingLotService;

    @Transactional(readOnly = true)
    public List<ParkingSpotResponse> getByLot(Long lotId) {
        // Validate lot exists
        parkingLotService.findById(lotId);
        return parkingSpotRepository.findByParkingLotIdAndActiveTrue(lotId)
                .stream().map(ParkingSpotResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ParkingSpotResponse getById(Long id) {
        return ParkingSpotResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public List<ParkingSpotResponse> getAvailable(Long lotId, LocalDateTime start, LocalDateTime end) {
        validateTimeRange(start, end);
        return parkingSpotRepository.findAvailableSpots(lotId, start, end)
                .stream().map(ParkingSpotResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ParkingSpot> findAnyAvailable(Long lotId, LocalDateTime start, LocalDateTime end) {
        validateTimeRange(start, end);
        return parkingSpotRepository.findAvailableSpots(lotId, start, end).stream().findFirst();
    }

    @Transactional
    public ParkingSpotResponse create(Long lotId, ParkingSpotRequest request) {
        ParkingLot lot = parkingLotService.findById(lotId);

        if (parkingSpotRepository.findByParkingLotIdAndSpotNumber(lotId, request.spotNumber()).isPresent()) {
            throw ParkioException.conflict(
                    "Spot '" + request.spotNumber() + "' already exists in lot " + lotId);
        }

        ParkingSpot spot = ParkingSpot.builder()
                .spotNumber(request.spotNumber())
                .type(request.type())
                .notes(request.notes())
                .parkingLot(lot)
                .build();

        return ParkingSpotResponse.from(parkingSpotRepository.save(spot));
    }

    @Transactional
    public ParkingSpotResponse create(Long lotId, Long userId, boolean internal, ParkingSpotRequest request) {
        parkingLotService.assertCanManage(lotId, userId, internal);
        return create(lotId, request);
    }

    @Transactional
    public List<ParkingSpotResponse> createBulk(Long lotId, Long userId, boolean internal, BulkSpotRequest request) {
        parkingLotService.assertCanManage(lotId, userId, internal);
        List<ParkingSpotResponse> spots = new java.util.ArrayList<>();
        for (int index = 1; index <= request.count(); index++) {
            String number = request.prefix() + String.format("%0" + request.padWidth() + "d", index);
            spots.add(create(lotId, new ParkingSpotRequest(number, request.type(), null)));
        }
        return spots;
    }

    @Transactional
    public ParkingSpotResponse update(Long id, ParkingSpotRequest request) {
        ParkingSpot spot = findById(id);
        spot.setSpotNumber(request.spotNumber());
        spot.setType(request.type());
        spot.setNotes(request.notes());
        return ParkingSpotResponse.from(parkingSpotRepository.save(spot));
    }

    @Transactional
    public ParkingSpotResponse update(Long id, Long userId, boolean internal, ParkingSpotRequest request) {
        ParkingSpot spot = findById(id);
        parkingLotService.assertCanManage(spot.getParkingLot().getId(), userId, internal);
        return update(id, request);
    }

    @Transactional
    public ParkingSpotResponse updateStatus(Long id, ParkingSpot.SpotStatus status) {
        ParkingSpot spot = findById(id);
        spot.setStatus(status);
        return ParkingSpotResponse.from(parkingSpotRepository.save(spot));
    }

    @Transactional
    public ParkingSpotResponse updateStatus(Long id, Long userId, boolean internal, ParkingSpot.SpotStatus status) {
        ParkingSpot spot = findById(id);
        parkingLotService.assertCanManage(spot.getParkingLot().getId(), userId, internal);
        return updateStatus(id, status);
    }

    @Transactional
    public void delete(Long id) {
        ParkingSpot spot = findById(id);
        spot.setActive(false);
        parkingSpotRepository.save(spot);
    }

    @Transactional
    public void delete(Long id, Long userId, boolean internal) {
        ParkingSpot spot = findById(id);
        parkingLotService.assertCanManage(spot.getParkingLot().getId(), userId, internal);
        delete(id);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public ParkingSpot findById(Long id) {
        return parkingSpotRepository.findById(id)
                .orElseThrow(() -> ParkioException.notFound("Parking spot not found: " + id));
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw ParkioException.badRequest("End time must be after start time");
        }
    }
}
