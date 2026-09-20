package com.example.parkio.service;

import com.example.parkio.dto.request.BookingRequest;
import com.example.parkio.dto.request.BookingExtensionRequest;
import com.example.parkio.dto.request.GuestCheckoutRequest;
import com.example.parkio.dto.request.WalkUpBookingRequest;
import com.example.parkio.dto.response.BookingResponse;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.entity.*;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.BookingRepository;
// import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final ParkingSpotService parkingSpotService;
    private final NotificationService notificationService;
    // @Lazy breaks the circular dependency: BookingService ↔ PricingService
    private final PricingService pricingService;

    public BookingService(BookingRepository bookingRepository,
                          UserService userService,
                          VehicleService vehicleService,
                          ParkingSpotService parkingSpotService,
                          NotificationService notificationService,
                          @Lazy PricingService pricingService) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.parkingSpotService = parkingSpotService;
        this.notificationService = notificationService;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getByUser(Long userId, Pageable pageable) {
        return PageResponse.from(
                bookingRepository.findByUserId(userId, pageable).map(BookingResponse::from));
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(Long id, Long userId, boolean isAdmin) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ParkioException.notFound("Booking not found: " + id));
        if (!isAdmin && !booking.getUser().getId().equals(userId)) {
            throw ParkioException.forbidden("Access denied to booking: " + id);
        }
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getByReference(String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> ParkioException.notFound("Booking not found: " + reference));
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse create(Long userId, BookingRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw ParkioException.badRequest("End time must be after start time");
        }

        User user = userService.findById(userId);
        Vehicle vehicle = vehicleService.findByIdRaw(request.vehicleId());

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw ParkioException.forbidden("Vehicle does not belong to this user");
        }

        ParkingSpot spot = parkingSpotService.findById(request.spotId());

        boolean hasConflict = !bookingRepository
                .findConflictingBookings(spot.getId(), request.startTime(), request.endTime())
                .isEmpty();
        if (hasConflict) {
            throw ParkioException.conflict("Spot is not available for the requested time window");
        }

        // Use pricing rules if available, fallback to simple hourly calc
        BigDecimal totalAmount = pricingService.calculateAmount(spot, request.startTime(), request.endTime());

        Booking booking = Booking.builder()
                .bookingReference(generateReference())
                .user(user)
                .vehicle(vehicle)
                .spot(spot)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .totalAmount(totalAmount)
                .notes(request.notes())
                .status(Booking.BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);

        notificationService.send(userId, Notification.NotificationType.BOOKING_CREATED,
                "Booking Created",
                "Your booking " + saved.getBookingReference() + " at " +
                spot.getParkingLot().getName() + " has been created.",
                saved.getId(), "Booking");

        return BookingResponse.from(saved);
    }

    @Transactional
    public BookingResponse confirm(Long id) {
        Booking booking = findById(id);
        assertStatus(booking, Booking.BookingStatus.PENDING, "confirm");
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);

        notificationService.send(booking.getUser().getId(),
                Notification.NotificationType.BOOKING_CONFIRMED,
                "Booking Confirmed",
                "Your booking " + booking.getBookingReference() + " is now confirmed.",
                booking.getId(), "Booking");

        return BookingResponse.from(saved);
    }

    @Transactional
    public BookingResponse checkIn(Long id, Long userId) {
        Booking booking = findById(id);
        if (!booking.getUser().getId().equals(userId)) {
            throw ParkioException.forbidden("Access denied");
        }
        assertStatus(booking, Booking.BookingStatus.CONFIRMED, "check in");
        booking.setActualCheckIn(LocalDateTime.now());
        booking.setStatus(Booking.BookingStatus.ACTIVE);
        booking.getSpot().setStatus(ParkingSpot.SpotStatus.OCCUPIED);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse checkOut(Long id, Long userId) {
        Booking booking = findById(id);
        if (!booking.getUser().getId().equals(userId)) {
            throw ParkioException.forbidden("Access denied");
        }
        assertStatus(booking, Booking.BookingStatus.ACTIVE, "check out");
        booking.setActualCheckOut(LocalDateTime.now());
        booking.setStatus(Booking.BookingStatus.COMPLETED);
        booking.getSpot().setStatus(ParkingSpot.SpotStatus.AVAILABLE);
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancel(Long id, Long userId, boolean isAdmin) {
        Booking booking = findById(id);
        if (!isAdmin && !booking.getUser().getId().equals(userId)) {
            throw ParkioException.forbidden("Access denied");
        }
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED
                || booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw ParkioException.badRequest("Booking cannot be cancelled in its current state");
        }
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        if (booking.getSpot().getStatus() == ParkingSpot.SpotStatus.OCCUPIED
                || booking.getSpot().getStatus() == ParkingSpot.SpotStatus.RESERVED) {
            booking.getSpot().setStatus(ParkingSpot.SpotStatus.AVAILABLE);
        }
        Booking saved = bookingRepository.save(booking);

        notificationService.send(booking.getUser().getId(),
                Notification.NotificationType.BOOKING_CANCELLED,
                "Booking Cancelled",
                "Your booking " + booking.getBookingReference() + " has been cancelled.",
                booking.getId(), "Booking");

        return BookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public java.util.List<Booking> findOverdueForNoShow(LocalDateTime cutoff) {
        return bookingRepository.findByStatusAndStartTimeBefore(Booking.BookingStatus.CONFIRMED, cutoff);
    }

    @Transactional
    public void markNoShow(Booking booking) {
        booking.setStatus(Booking.BookingStatus.NO_SHOW);
        booking.getSpot().setStatus(ParkingSpot.SpotStatus.AVAILABLE);
        bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public java.util.List<Booking> findDueForReminder(LocalDateTime start, LocalDateTime end) {
        return bookingRepository.findByStatusAndReminderSentFalseAndStartTimeBetween(
                Booking.BookingStatus.CONFIRMED, start, end);
    }

    @Transactional
    public void markReminderSent(Booking booking) {
        booking.setReminderSent(true);
        bookingRepository.save(booking);
    }

    @Transactional
    public BookingResponse extend(Long id, Long userId, BookingExtensionRequest request) {
        Booking booking = findById(id);
        if (!booking.getUser().getId().equals(userId)) throw ParkioException.forbidden("Access denied");
        if (request.newEndTime().isBefore(booking.getEndTime()) || !request.newEndTime().isAfter(booking.getStartTime()))
            throw ParkioException.badRequest("Extension must move the booking end time forward");
        if (!bookingRepository.findConflictingBookings(booking.getSpot().getId(), booking.getEndTime(), request.newEndTime())
                .stream().filter(other -> !other.getId().equals(id)).toList().isEmpty())
            throw ParkioException.conflict("Spot is not available for the extension");
        booking.setEndTime(request.newEndTime());
        booking.setTotalAmount(pricingService.calculateAmount(booking.getSpot(), booking.getStartTime(), request.newEndTime()));
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse createGuestQrBooking(Long spaceId, GuestCheckoutRequest request) {
        User user = userService.findOrCreateGuestByPhone(request.driverPhone(), request.driverName());
        Vehicle vehicle = vehicleService.findOrCreateByPlate(request.vehiclePlate(), request.vehicleType(), user);
        ParkingSpot spot = parkingSpotService.findAnyAvailable(spaceId, request.startTime(), request.endTime())
                .orElseThrow(() -> ParkioException.conflict("No parking spot is available"));
        Booking booking = Booking.builder().bookingReference(generateReference()).user(user).vehicle(vehicle).spot(spot)
                .startTime(request.startTime()).endTime(request.endTime())
                .totalAmount(pricingService.calculateAmount(spot, request.startTime(), request.endTime()))
                .promoCode(request.promoCode()).initiatedBy(Booking.InitiatedBy.QR_SCAN).status(Booking.BookingStatus.PENDING).build();
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse createWalkUp(Long actingUserId, WalkUpBookingRequest request) {
        User user = userService.findOrCreateGuestByPhone(request.driverPhone(), request.driverName());
        Vehicle vehicle = vehicleService.findOrCreateByPlate(request.vehiclePlate(), request.vehicleType(), user);
        ParkingSpot spot = parkingSpotService.findAnyAvailable(request.spaceId(), request.startTime(), request.endTime())
                .orElseThrow(() -> ParkioException.conflict("No parking spot is available"));
        Booking booking = Booking.builder().bookingReference(generateReference()).user(user).vehicle(vehicle).spot(spot)
                .startTime(request.startTime()).endTime(request.endTime()).paymentTiming(request.paymentTiming())
                .initiatedBy(Booking.InitiatedBy.WARDEN_APP)
                .totalAmount(pricingService.calculateAmount(spot, request.startTime(), request.endTime()))
                .status(Booking.BookingStatus.PENDING).build();
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional
    public Booking staffCheckIn(Long id, Staff staff) {
        Booking booking = findById(id);
        assertStaffOwnsBooking(booking, staff);
        assertStatus(booking, Booking.BookingStatus.CONFIRMED, "check in");
        booking.setActualCheckIn(LocalDateTime.now()); booking.setStatus(Booking.BookingStatus.ACTIVE);
        booking.getSpot().setStatus(ParkingSpot.SpotStatus.OCCUPIED);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking staffCheckOut(Long id, Staff staff) {
        Booking booking = findById(id);
        assertStaffOwnsBooking(booking, staff);
        assertStatus(booking, Booking.BookingStatus.ACTIVE, "check out");
        booking.setActualCheckOut(LocalDateTime.now()); booking.setStatus(Booking.BookingStatus.COMPLETED);
        booking.getSpot().setStatus(ParkingSpot.SpotStatus.AVAILABLE);
        return bookingRepository.save(booking);
    }

    private void assertStaffOwnsBooking(Booking booking, Staff staff) {
        if (booking.getSpot().getParkingLot().getOrganization() == null ||
                !booking.getSpot().getParkingLot().getOrganization().getId().equals(staff.getOrganization().getId()))
            throw ParkioException.forbidden("Staff member cannot manage this booking");
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> ParkioException.notFound("Booking not found: " + id));
    }

    private void assertStatus(Booking booking, Booking.BookingStatus expected, String action) {
        if (booking.getStatus() != expected) {
            throw ParkioException.badRequest(
                    "Cannot " + action + " a booking with status: " + booking.getStatus());
        }
    }

    private String generateReference() {
        return "PKI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
