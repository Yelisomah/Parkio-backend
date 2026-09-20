package com.example.parkio.controller;

import com.example.parkio.base.BaseIntegrationTest;
import com.example.parkio.dto.request.ParkingLotRequest;
import com.example.parkio.entity.ParkingLot;
import com.example.parkio.repository.ParkingLotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ParkingLotControllerTest extends BaseIntegrationTest {

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    private ParkingLot testLot;

    @BeforeEach
    void setupLot() {
        testLot = parkingLotRepository.save(ParkingLot.builder()
                .name("Central Park Lot")
                .address("1 Main St")
                .city("Accra")
                .state("Greater Accra")
                .hourlyRate(new BigDecimal("5.00"))
                .approvalStatus(ParkingLot.ApprovalStatus.APPROVED) // pre-approval-workflow fixtures need this explicit now, else PENDING_APPROVAL hides it from public queries
                .build());
    }

    @Test
    void getAll_public_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/parking-lots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getById_public_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/parking-lots/" + testLot.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Central Park Lot"))
                .andExpect(jsonPath("$.data.city").value("Accra"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/parking-lots/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_byKeyword_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/parking-lots/search?keyword=Central"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void create_asAdmin_returns201() throws Exception {
        ParkingLotRequest req = new ParkingLotRequest(
                "New Lot", "5 Second St", "Kumasi", "Ashanti",
                null, null, null,
                new BigDecimal("8.00"), null,
                null, null, "Test lot", null, null, null);

        mockMvc.perform(post("/api/v1/parking-lots")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("New Lot"));
    }

    @Test
    void create_asUser_returns201_asIndividualOwner() throws Exception {
        // Ownership model change: any authenticated user can now list a space
        // individually (pending approval) — this used to be admin-only.
        ParkingLotRequest req = new ParkingLotRequest(
                "Individually Owned Lot", "6 Third St", "Tamale", "Northern",
                null, null, null, new BigDecimal("4.00"), null,
                null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/parking-lots")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Individually Owned Lot"))
                .andExpect(jsonPath("$.data.approvalStatus").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.ownerUserId").isNotEmpty());
    }

    @Test
    void create_withoutAuth_returns403() throws Exception {
        ParkingLotRequest req = new ParkingLotRequest(
                "No Auth Lot", "7 Fourth St", "Ho", "Volta",
                null, null, null, new BigDecimal("3.00"), null,
                null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/parking-lots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivate_asAdmin_returns200() throws Exception {
        mockMvc.perform(patch("/api/v1/parking-lots/" + testLot.getId() + "/deactivate")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Parking lot deactivated"));
    }

    @Test
    void activate_asAdmin_returns200() throws Exception {
        // deactivate first
        testLot.setActive(false);
        parkingLotRepository.save(testLot);

        mockMvc.perform(patch("/api/v1/parking-lots/" + testLot.getId() + "/activate")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk());
    }
}
