package com.example.parkio.controller;

import com.example.parkio.base.BaseIntegrationTest;
import com.example.parkio.dto.request.LoginRequest;
import com.example.parkio.dto.request.RegisterRequest;
import com.example.parkio.dto.request.RequestOtpRequest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    @Test
    void register_success_withVerifiedPhone() throws Exception {
        // Full flow: request OTP (dev-mode returns it directly), then register with it.
        String otpResponse = mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestOtpRequest("+233500000001"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String otpCode = JsonPath.read(otpResponse, "$.data");

        RegisterRequest req = new RegisterRequest(
                "John", "Doe", "john.doe@test.com",
                "SecurePass1!", "+233500000001", otpCode);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("john.doe@test.com"));
    }

    @Test
    void register_success_withoutPhone() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "Jane", "NoPhone", "jane.nophone@test.com",
                "SecurePass1!", null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.email").value("jane.nophone@test.com"));
    }

    @Test
    void register_phoneWithoutOtp_returns400() throws Exception {
        // A phone was supplied but no otpCode — should fail validation, not silently register unverified.
        RegisterRequest req = new RegisterRequest(
                "No", "Otp", "no.otp@test.com",
                "SecurePass1!", "+233500000002", null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_phoneWithWrongOtp_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestOtpRequest("+233500000003"))))
                .andExpect(status().isOk());

        RegisterRequest req = new RegisterRequest(
                "Wrong", "Otp", "wrong.otp@test.com",
                "SecurePass1!", "+233500000003", "000000");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        // testUser already exists from BaseIntegrationTest
        RegisterRequest req = new RegisterRequest(
                "Test", "User", testUser.getEmail(),
                "SecurePass1!", null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "Bad", "Email", "not-an-email", "password123", null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "Bad", "Pass", "valid@email.com", "short", null, null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_success() throws Exception {
        LoginRequest req = new LoginRequest(testUser.getEmail(), "password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest(testUser.getEmail(), "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        LoginRequest req = new LoginRequest("nobody@example.com", "password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logout_withValidToken_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
