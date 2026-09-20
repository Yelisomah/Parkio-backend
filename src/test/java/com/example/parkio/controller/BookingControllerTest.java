package com.example.parkio.controller;

import com.example.parkio.base.BaseIntegrationTest;
import com.example.parkio.dto.request.BookingRequest;
import com.example.parkio.entity.*;
import com.example.parkio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingControllerTest extends BaseIntegrationTest {

    @Autowired private ParkingLotRepository lotRepository;
    @Autowired private ParkingSpotRepository spotRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private BookingRepository bookingRepository;

    private ParkingSpot spot;
    private Vehicle vehicle;

    @BeforeEach
    void setupBookingFixtures() {
        ParkingLot lot = lotRepository.save(ParkingLot.builder()
                .name("Test Lot")
                .address("1 Test Ave")
                .city("Accra")
                .state("Greater Accra")
                .hourlyRate(new BigDecimal("5.00"))
                .build());

        spot = spotRepository.save(ParkingSpot.builder()
                .spotNumber("A01")
                .type(ParkingSpot.SpotType.STANDARD)
                .parkingLot(lot)
                .build());

        vehicle = vehicleRepository.save(Vehicle.builder()
                .licensePlate("GR-1234-25")
                .make("Toyota")
                .model("Camry")
                .year("2022")
                .type(Vehicle.VehicleType.SEDAN)
                .owner(testUser)
                .build());
    }

    private BookingRequest futureBooking() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        return new BookingRequest(vehicle.getId(), spot.getId(), start, start.plusHours(3), null, null);
    }

    @Test
    void createBooking_authenticated_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(futureBooking())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookingReference", notNullValue()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").isNumber());
    }

    @Test
    void createBooking_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(futureBooking())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBooking_pastTime_returns400() throws Exception {
        LocalDateTime past = LocalDateTime.now().minusHours(1);
        BookingRequest req = new BookingRequest(vehicle.getId(), spot.getId(), past, past.plusHours(2), null, null);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyBookings_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void cancelBooking_byOwner_returns200() throws Exception {
        // Create booking first
        String body = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(futureBooking())))
                .andReturn().getResponse().getContentAsString();

        Long bookingId = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/v1/bookings/" + bookingId + "/cancel")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void confirmBooking_asAdmin_returns200() throws Exception {
        // Create booking
        String body = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(futureBooking())))
                .andReturn().getResponse().getContentAsString();

        Long bookingId = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/v1/bookings/" + bookingId + "/confirm")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void confirmBooking_asUser_returns403() throws Exception {
        String body = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(futureBooking())))
                .andReturn().getResponse().getContentAsString();

        Long bookingId = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/v1/bookings/" + bookingId + "/confirm")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void conflictingBooking_returns409() throws Exception {
        // First booking
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(futureBooking())))
                .andExpect(status().isCreated());

        // Second booking for same spot & time
        mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(futureBooking())))
                .andExpect(status().isConflict());
    }
}
