package com.example.parkio.repository;

import com.example.parkio.entity.ParkingLot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies ParkingLotRepository.findNearbyWithDistance against a REAL
 * Postgres+PostGIS instance — per tech-standards.md: "Any geospatial ...
 * logic requires a Testcontainers-backed integration test against real
 * PostgreSQL/PostGIS ... do not rely solely on mocked repositories/H2".
 *
 * Requires Docker to be available and network access to pull the
 * postgis/postgis image — NOT exercised in the sandbox this was written in.
 * Run it yourself (`./gradlew test --tests ParkingLotNearbySearchIT`) before
 * relying on the /parking-lots/nearby endpoint in production.
 */
@Testcontainers
@SpringBootTest
class ParkingLotNearbySearchIT {

    @Container
    static PostgreSQLContainer<?> postgis = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgis::getJdbcUrl);
        registry.add("spring.datasource.username", postgis::getUsername);
        registry.add("spring.datasource.password", postgis::getPassword);
        // Real Flyway migrations (V1 + V2) run against this container — exactly
        // what will happen in prod, unlike the H2 create-drop test profile.
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    // Accra, Ghana coordinates — two lots ~1.5km apart, one ~20km away
    private static final double ACCRA_LAT = 5.6037;
    private static final double ACCRA_LNG = -0.1870;

    private static Long nearLotId;
    private static Long farLotId;

    @BeforeAll
    static void seedIsHandledPerTest() {
        // Intentionally empty — seeding happens in the test method itself via
        // the repository, so each run is self-contained and idempotent.
    }

    @Test
    void findsOnlyLotsWithinRadius_orderedByDistance() {
        ParkingLot near = parkingLotRepository.save(ParkingLot.builder()
                .name("Near Lot").address("Test Address 1").city("Accra").state("Greater Accra")
                .latitude(BigDecimal.valueOf(5.6100)).longitude(BigDecimal.valueOf(-0.1800))
                .hourlyRate(BigDecimal.valueOf(5)).build());

        ParkingLot far = parkingLotRepository.save(ParkingLot.builder()
                .name("Far Lot").address("Test Address 2").city("Accra").state("Greater Accra")
                .latitude(BigDecimal.valueOf(5.8000)).longitude(BigDecimal.valueOf(-0.3000))
                .hourlyRate(BigDecimal.valueOf(5)).build());

        List<ParkingLotRepository.NearbyLotProjection> results =
                parkingLotRepository.findNearbyWithDistance(ACCRA_LAT, ACCRA_LNG, 5_000); // 5km radius

        List<Long> resultIds = results.stream().map(ParkingLotRepository.NearbyLotProjection::getId).toList();

        assertThat(resultIds).contains(near.getId());
        assertThat(resultIds).doesNotContain(far.getId()); // ~20km away, outside 5km radius

        // Nearest-first ordering
        if (results.size() > 1) {
            for (int i = 1; i < results.size(); i++) {
                assertThat(results.get(i).getDistanceMeters())
                        .isGreaterThanOrEqualTo(results.get(i - 1).getDistanceMeters());
            }
        }
    }

    @Test
    void excludesLotsWithNoCoordinates() {
        ParkingLot noCoords = parkingLotRepository.save(ParkingLot.builder()
                .name("No Coords Lot").address("Test Address 3").city("Accra").state("Greater Accra")
                .hourlyRate(BigDecimal.valueOf(5)).build());

        List<ParkingLotRepository.NearbyLotProjection> results =
                parkingLotRepository.findNearbyWithDistance(ACCRA_LAT, ACCRA_LNG, 50_000);

        assertThat(results.stream().map(ParkingLotRepository.NearbyLotProjection::getId))
                .doesNotContain(noCoords.getId());
    }
}
