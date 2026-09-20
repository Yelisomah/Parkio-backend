package com.example.parkio.dto.response;

import com.example.parkio.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String transactionId,
        Long bookingId,
        String bookingReference,
        BigDecimal amount,
        String paymentMethod,
        String status,
        String gatewayReference,
        String failureReason,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        BigDecimal refundAmount,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getTransactionId(),
                p.getBooking().getId(),
                p.getBooking().getBookingReference(),
                p.getAmount(),
                p.getPaymentMethod().name(),
                p.getStatus().name(),
                p.getGatewayReference(),
                p.getFailureReason(),
                p.getPaidAt(),
                p.getRefundedAt(),
                p.getRefundAmount(),
                p.getCreatedAt()
        );
    }
}
