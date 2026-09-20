package com.example.parkio.base;

import com.example.parkio.entity.Role;
import com.example.parkio.entity.User;
import com.example.parkio.repository.RoleRepository;
import com.example.parkio.repository.UserRepository;
import com.example.parkio.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected RoleRepository roleRepository;
    @Autowired protected JwtUtil jwtUtil;

    protected User testUser;
    protected User adminUser;
    protected String userToken;
    protected String adminToken;

    @BeforeEach
    void setupBase() {
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Role.RoleName.ROLE_USER).build()));
        Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(Role.RoleName.ROLE_ADMIN).build()));

        testUser = userRepository.save(User.builder()
                .firstName("Test")
                .lastName("User")
                .email("testuser@parkio.test")
                .password("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG") // "password"
                .roles(Set.of(userRole))
                .build());

        adminUser = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@parkio.test")
                .password("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG")
                .roles(Set.of(adminRole))
                .build());

        userToken  = "Bearer " + generateToken(testUser.getEmail(),  "ROLE_USER");
        adminToken = "Bearer " + generateToken(adminUser.getEmail(), "ROLE_ADMIN");
    }

    private String generateToken(String email, String role) {
        UserDetails ud = new org.springframework.security.core.userdetails.User(
                email, "", List.of(new SimpleGrantedAuthority(role)));
        return jwtUtil.generateAccessToken(ud);
    }

    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
