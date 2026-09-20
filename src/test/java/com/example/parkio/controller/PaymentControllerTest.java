package com.example.parkio.controller;

import com.example.parkio.base.BaseIntegrationTest;
import com.example.parkio.dto.request.BookingRequest;
import com.example.parkio.dto.request.PaymentRequest;
import com.example.parkio.entity.*;
import com.example.parkio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerTest extends BaseIntegrationTest {

    @Autowired private ParkingLotRepository lotRepository;
    @Autowired private ParkingSpotRepository spotRepository;
    @Autowired private VehicleRepository vehicleRepository;

    private Long bookingId;

    @BeforeEach
    void setupPaymentFixtures() throws Exception {
        ParkingLot lot = lotRepository.save(ParkingLot.builder()
                .name("Pay Lot")
                .address("1 Pay St")
                .city("Accra")
                .state("Greater Accra")
                .hourlyRate(new BigDecimal("10.00"))
                .build());

        ParkingSpot spot = spotRepository.save(ParkingSpot.builder()
                .spotNumber("P01")
                .type(ParkingSpot.SpotType.STANDARD)
                .parkingLot(lot)
                .build());

        Vehicle vehicle = vehicleRepository.save(Vehicle.builder()
                .licensePlate("GR-PAY-25")
                .make("Honda")
                .model("Civic")
                .year("2020")
                .type(Vehicle.VehicleType.SEDAN)
                .owner(testUser)
                .build());

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        BookingRequest booking = new BookingRequest(vehicle.getId(), spot.getId(),
                start, start.plusHours(2), null, null);

        String resp = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(booking)))
                .andReturn().getResponse().getContentAsString();

        bookingId = objectMapper.readTree(resp).path("data").path("id").asLong();
    }

    @Test
    void processPayment_success_returns201() throws Exception {
        PaymentRequest req = new PaymentRequest(bookingId, Payment.PaymentMethod.CREDIT_CARD, "tok_test_123");

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.transactionId").isNotEmpty());
    }

    @Test
    void processPayment_unauthenticated_returns403() throws Exception {
        PaymentRequest req = new PaymentRequest(bookingId, Payment.PaymentMethod.CASH, null);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void processPayment_duplicate_returns409() throws Exception {
        PaymentRequest req = new PaymentRequest(bookingId, Payment.PaymentMethod.MOBILE_MONEY, null);

        // First payment
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated());

        // Duplicate payment
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void getPaymentByBooking_returns200() throws Exception {
        // Pay first
        PaymentRequest req = new PaymentRequest(bookingId, Payment.PaymentMethod.DEBIT_CARD, null);
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated());

        // Then retrieve
        mockMvc.perform(get("/api/v1/payments/booking/" + bookingId)
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingId").value(bookingId));
    }

    @Test
    void getPaymentById_asNonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/payments/1")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void refund_asAdmin_returns200() throws Exception {
        // Pay first
        PaymentRequest req = new PaymentRequest(bookingId, Payment.PaymentMethod.WALLET, null);
        String resp = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andReturn().getResponse().getContentAsString();

        Long paymentId = objectMapper.readTree(resp).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }
}
