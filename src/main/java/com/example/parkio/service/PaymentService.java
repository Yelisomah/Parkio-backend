package com.example.parkio.service;

import com.example.parkio.dto.request.PaymentRequest;
import com.example.parkio.dto.response.PaymentResponse;
import com.example.parkio.entity.AuditLog;
import com.example.parkio.entity.Booking;
import com.example.parkio.entity.Notification;
import com.example.parkio.entity.Payment;
import com.example.parkio.entity.Staff;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.payment.PaymentGateway;
import com.example.parkio.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository  paymentRepository;
    private final BookingService     bookingService;
    private final PaymentGateway     paymentGateway;
    private final NotificationService notificationService;
    private final AuditService       auditService;

    @Value("${app.payment.currency:GHS}")
    private String currency;

    // ── Process payment ───────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse processPayment(Long userId, PaymentRequest request) {
        Booking booking = bookingService.findById(request.bookingId());

        if (!booking.getUser().getId().equals(userId)) {
            throw ParkioException.forbidden("Access denied to this booking");
        }
        if (booking.getStatus() != Booking.BookingStatus.PENDING
                && booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw ParkioException.badRequest(
                    "Cannot pay for a booking with status: " + booking.getStatus());
        }
        if (paymentRepository.findByBookingId(request.bookingId()).isPresent()) {
            throw ParkioException.conflict("Payment already exists for this booking");
        }

        // Create payment in PROCESSING state
        Payment payment = Payment.builder()
                .transactionId(generateTransactionId())
                .booking(booking)
                .amount(booking.getTotalAmount())
                .paymentMethod(request.paymentMethod())
                .status(Payment.PaymentStatus.PROCESSING)
                .gatewayReference(request.gatewayToken())
                .build();
        payment = paymentRepository.save(payment);

        // Call gateway
        String description = "Parking booking " + booking.getBookingReference();
        PaymentGateway.ChargeResult result = paymentGateway.charge(
                request.gatewayToken(), booking.getTotalAmount(), currency, description);

        if (result.status() == PaymentGateway.ChargeStatus.SUCCESS) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setGatewayReference(result.gatewayReference());
            payment.setPaidAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            // Auto-confirm booking on successful payment
            if (booking.getStatus() == Booking.BookingStatus.PENDING) {
                bookingService.confirm(booking.getId());
            }

            notificationService.send(userId, Notification.NotificationType.PAYMENT_SUCCESS,
                    "Payment Successful",
                    String.format("Payment of %s %s for booking %s was successful.",
                            currency, booking.getTotalAmount(), booking.getBookingReference()),
                    payment.getId(), "Payment");

            auditService.log(booking.getUser().getEmail(),
                    AuditLog.AuditAction.PAYMENT_PROCESSED, "Payment", payment.getId(),
                    "Payment processed for booking " + booking.getBookingReference());

        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(result.failureReason());
            payment = paymentRepository.save(payment);

            notificationService.send(userId, Notification.NotificationType.PAYMENT_FAILED,
                    "Payment Failed",
                    "Your payment for booking " + booking.getBookingReference() +
                    " failed: " + result.failureReason(),
                    payment.getId(), "Payment");

            throw ParkioException.badRequest("Payment failed: " + result.failureReason());
        }

        return PaymentResponse.from(payment);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentResponse getByBooking(Long bookingId, Long userId) {
        Booking booking = bookingService.findById(bookingId);
        if (!booking.getUser().getId().equals(userId)) {
            throw ParkioException.forbidden("Access denied");
        }
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> ParkioException.notFound("No payment found for booking: " + bookingId));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return PaymentResponse.from(paymentRepository.findById(id)
                .orElseThrow(() -> ParkioException.notFound("Payment not found: " + id)));
    }

    // ── Refund ────────────────────────────────────────────────────────────────

    @Transactional
    public PaymentResponse refund(Long id, String actorEmail) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> ParkioException.notFound("Payment not found: " + id));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw ParkioException.badRequest("Only completed payments can be refunded");
        }

        PaymentGateway.ChargeResult result =
                paymentGateway.refund(payment.getGatewayReference(), payment.getAmount());

        if (result.status() == PaymentGateway.ChargeStatus.SUCCESS) {
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            payment.setRefundAmount(payment.getAmount());
            payment.setRefundedAt(LocalDateTime.now());

            Booking booking = payment.getBooking();
            booking.setStatus(Booking.BookingStatus.CANCELLED);

            payment = paymentRepository.save(payment);

            notificationService.send(booking.getUser().getId(),
                    Notification.NotificationType.PAYMENT_REFUNDED,
                    "Payment Refunded",
                    "Your payment for booking " + booking.getBookingReference() + " has been refunded.",
                    payment.getId(), "Payment");

            auditService.log(actorEmail, AuditLog.AuditAction.PAYMENT_REFUNDED,
                    "Payment", payment.getId(),
                    "Refund issued for booking " + booking.getBookingReference());
        } else {
            throw ParkioException.badRequest("Refund failed: " + result.failureReason());
        }

        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse initiatePhonePush(Long bookingId, String phone, String momoProvider) {
        Booking booking = bookingService.findById(bookingId);
        Payment payment = Payment.builder().transactionId(generateTransactionId()).booking(booking)
                .amount(booking.getTotalAmount()).paymentMethod(Payment.PaymentMethod.MOBILE_MONEY)
                .status(Payment.PaymentStatus.PROCESSING).build();
        PaymentGateway.ChargeResult result = paymentGateway.chargeByPhone(phone, momoProvider,
                booking.getTotalAmount(), currency, "Parking booking " + booking.getBookingReference());
        payment.setGatewayReference(result.gatewayReference());
        if (result.status() == PaymentGateway.ChargeStatus.SUCCESS) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED); payment.setPaidAt(LocalDateTime.now());
            if (booking.getStatus() == Booking.BookingStatus.PENDING) bookingService.confirm(bookingId);
        } else if (result.status() == PaymentGateway.ChargeStatus.FAILED) {
            payment.setStatus(Payment.PaymentStatus.FAILED); payment.setFailureReason(result.failureReason());
        }
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse logCashPayment(Long bookingId, Staff staff) {
        Booking booking = bookingService.findById(bookingId);
        if (booking.getSpot().getParkingLot().getOrganization() == null ||
                !booking.getSpot().getParkingLot().getOrganization().getId().equals(staff.getOrganization().getId()))
            throw ParkioException.forbidden("Staff member cannot collect payment for this booking");
        Payment payment = Payment.builder().transactionId(generateTransactionId()).booking(booking)
                .amount(booking.getTotalAmount()).paymentMethod(Payment.PaymentMethod.CASH)
                .status(Payment.PaymentStatus.COMPLETED).gatewayReference("CASH-LOGGED-STAFF-" + staff.getId())
                .paidAt(LocalDateTime.now()).build();
        if (booking.getStatus() == Booking.BookingStatus.PENDING) bookingService.confirm(bookingId);
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse finalizeFromWebhook(String reference, boolean success, String failureReason) {
        Payment payment = paymentRepository.findByGatewayReference(reference)
                .orElseThrow(() -> ParkioException.notFound("Payment not found: " + reference));
        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED || payment.getStatus() == Payment.PaymentStatus.FAILED)
            return PaymentResponse.from(payment);
        if (success) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED); payment.setPaidAt(LocalDateTime.now());
            if (payment.getBooking().getStatus() == Booking.BookingStatus.PENDING)
                bookingService.confirm(payment.getBooking().getId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED); payment.setFailureReason(failureReason);
        }
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
