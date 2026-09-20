package com.example.parkio.service;

import com.example.parkio.dto.request.ParkingLotRequest;
import com.example.parkio.dto.response.NearbyLotResponse;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.dto.response.ParkingLotResponse;
import com.example.parkio.entity.ParkingLot;
import com.example.parkio.entity.ParkingSpot;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.ParkingLotRepository;
import com.example.parkio.repository.ParkingSpotRepository;
import com.example.parkio.repository.StaffRepository;
import com.example.parkio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public PageResponse<ParkingLotResponse> getAll(Pageable pageable) {
        return PageResponse.from(
                parkingLotRepository.findByActiveTrueAndApprovalStatus(ParkingLot.ApprovalStatus.APPROVED, pageable)
                        .map(lot -> buildResponse(lot)));
    }

    @Transactional(readOnly = true)
    public ParkingLotResponse getById(Long id) {
        ParkingLot lot = findById(id);
        return buildResponse(lot);
    }

    @Transactional(readOnly = true)
    public PageResponse<ParkingLotResponse> search(String keyword, Pageable pageable) {
        return PageResponse.from(
                parkingLotRepository.searchByKeyword(keyword, pageable)
                        .map(this::buildResponse));
    }

    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getByCity(String city) {
        return parkingLotRepository.findByCityIgnoreCaseAndActiveTrueAndApprovalStatus(
                city, ParkingLot.ApprovalStatus.APPROVED)
                .stream().map(this::buildResponse).toList();
    }

    @Transactional
    public ParkingLotResponse create(ParkingLotRequest request) {
        ParkingLot lot = ParkingLot.builder()
                .name(request.name())
                .address(request.address())
                .city(request.city())
                .state(request.state())
                .zipCode(request.zipCode())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .hourlyRate(request.hourlyRate())
                .dailyRate(request.dailyRate())
                .openTime(request.openTime())
                .closeTime(request.closeTime())
                .description(request.description())
                .amenities(request.amenities())
                .build();

        return buildResponse(parkingLotRepository.save(lot));
    }

    @Transactional
    public ParkingLotResponse create(Long userId, boolean internal, ParkingLotRequest request) {
        ParkingLot lot = ParkingLot.builder()
                .name(request.name()).address(request.address()).city(request.city()).state(request.state())
                .zipCode(request.zipCode()).latitude(request.latitude()).longitude(request.longitude())
                .hourlyRate(request.hourlyRate()).dailyRate(request.dailyRate()).openTime(request.openTime())
                .closeTime(request.closeTime()).description(request.description()).amenities(request.amenities())
                .managementMode(request.managementMode()).build();
        assignOwnership(lot, userId, internal, request.organizationId());
        return buildResponse(parkingLotRepository.save(lot));
    }

    @Transactional
    public ParkingLotResponse update(Long id, ParkingLotRequest request) {
        ParkingLot lot = findById(id);

        lot.setName(request.name());
        lot.setAddress(request.address());
        lot.setCity(request.city());
        lot.setState(request.state());
        lot.setZipCode(request.zipCode());
        lot.setLatitude(request.latitude());
        lot.setLongitude(request.longitude());
        lot.setHourlyRate(request.hourlyRate());
        lot.setDailyRate(request.dailyRate());
        lot.setOpenTime(request.openTime());
        lot.setCloseTime(request.closeTime());
        lot.setDescription(request.description());
        lot.setAmenities(request.amenities());

        return buildResponse(parkingLotRepository.save(lot));
    }

    @Transactional
    public ParkingLotResponse update(Long id, Long userId, boolean internal, ParkingLotRequest request) {
        ParkingLot lot = findById(id);
        assertCanManage(lot, userId, internal);
        lot.setName(request.name()); lot.setAddress(request.address()); lot.setCity(request.city());
        lot.setState(request.state()); lot.setZipCode(request.zipCode()); lot.setLatitude(request.latitude());
        lot.setLongitude(request.longitude()); lot.setHourlyRate(request.hourlyRate()); lot.setDailyRate(request.dailyRate());
        lot.setOpenTime(request.openTime()); lot.setCloseTime(request.closeTime());
        lot.setDescription(request.description()); lot.setAmenities(request.amenities());
        lot.setManagementMode(request.managementMode());
        return buildResponse(parkingLotRepository.save(lot));
    }

    @Transactional
    public void deactivate(Long id) {
        ParkingLot lot = findById(id);
        lot.setActive(false);
        parkingLotRepository.save(lot);
    }

    @Transactional
    public void deactivate(Long id, Long userId, boolean internal) {
        ParkingLot lot = findById(id); assertCanManage(lot, userId, internal);
        lot.setActive(false); parkingLotRepository.save(lot);
    }

    @Transactional
    public void activate(Long id) {
        ParkingLot lot = findById(id);
        lot.setActive(true);
        parkingLotRepository.save(lot);
    }

    @Transactional
    public void activate(Long id, Long userId, boolean internal) {
        ParkingLot lot = findById(id); assertCanManage(lot, userId, internal);
        lot.setActive(true); parkingLotRepository.save(lot);
    }

    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getMine(Long userId) {
        Map<Long, ParkingLot> lots = new LinkedHashMap<>();
        parkingLotRepository.findByOwnerUserId(userId).forEach(lot -> lots.put(lot.getId(), lot));
        staffRepository.findByUserIdAndActiveTrue(userId).forEach(staff ->
                parkingLotRepository.findByOrganizationId(staff.getOrganization().getId())
                        .forEach(lot -> lots.put(lot.getId(), lot)));
        return lots.values().stream().map(this::buildResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NearbyLotResponse> getNearby(double lat, double lng, double radiusKm) {
        if (radiusKm <= 0) throw ParkioException.badRequest("Radius must be positive");
        return parkingLotRepository.findNearbyWithDistance(lat, lng, radiusKm * 1000)
                .stream().map(result -> new NearbyLotResponse(
                        buildResponse(findById(result.getId())), result.getDistanceMeters() / 1000.0)).toList();
    }

    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getPendingApproval() {
        return parkingLotRepository.findByApprovalStatusOrderByCreatedAtAsc(ParkingLot.ApprovalStatus.PENDING_APPROVAL)
                .stream().map(this::buildResponse).toList();
    }

    @Transactional
    public ParkingLotResponse approve(Long id, String reviewer) {
        ParkingLot lot = findById(id);
        lot.setApprovalStatus(ParkingLot.ApprovalStatus.APPROVED);
        return buildResponse(parkingLotRepository.save(lot));
    }

    @Transactional
    public ParkingLotResponse reject(Long id, String reviewer, String reason) {
        if (reason == null || reason.isBlank()) throw ParkioException.badRequest("Rejection reason is required");
        ParkingLot lot = findById(id);
        lot.setApprovalStatus(ParkingLot.ApprovalStatus.CLOSED);
        return buildResponse(parkingLotRepository.save(lot));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public ParkingLot findById(Long id) {
        return parkingLotRepository.findById(id)
                .orElseThrow(() -> ParkioException.notFound("Parking lot not found: " + id));
    }

    private void assignOwnership(ParkingLot lot, Long userId, boolean internal, Long organizationId) {
        if (organizationId == null) {
            if (!internal) lot.setOwnerUser(userRepository.findById(userId)
                    .orElseThrow(() -> ParkioException.notFound("User not found: " + userId)));
            return;
        }
        if (!staffRepository.existsByOrganizationIdAndUserIdAndActiveTrue(organizationId, userId))
                throw ParkioException.forbidden("Only organization staff can list for this organization");
        var staff = staffRepository.findByOrganizationIdAndUserIdAndActiveTrue(organizationId, userId).orElseThrow();
        if (staff.getRole() != com.example.parkio.entity.Staff.StaffRole.ORG_ADMIN && !internal)
            throw ParkioException.forbidden("Only organization admins can list for this organization");
        lot.setOrganization(staff.getOrganization());
    }

    public void assertCanManage(Long lotId, Long userId, boolean internal) {
        assertCanManage(findById(lotId), userId, internal);
    }

    private void assertCanManage(ParkingLot lot, Long userId, boolean internal) {
        if (internal) return;
        boolean owner = lot.getOwnerUser() != null && lot.getOwnerUser().getId().equals(userId);
        boolean orgAdmin = lot.getOrganization() != null &&
                staffRepository.findByOrganizationIdAndUserIdAndActiveTrue(lot.getOrganization().getId(), userId)
                        .map(staff -> staff.getRole() == com.example.parkio.entity.Staff.StaffRole.ORG_ADMIN).orElse(false);
        if (!owner && !orgAdmin) throw ParkioException.forbidden("You do not manage this parking lot");
    }

    private ParkingLotResponse buildResponse(ParkingLot lot) {
        long total = parkingSpotRepository.countByParkingLotIdAndActiveTrue(lot.getId());
        long available = parkingSpotRepository.countByParkingLotIdAndStatus(
                lot.getId(), ParkingSpot.SpotStatus.AVAILABLE);
        return ParkingLotResponse.from(lot, total, available);
    }
}
