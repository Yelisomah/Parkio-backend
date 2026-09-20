package com.example.parkio.repository;

import com.example.parkio.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwnerId(Long ownerId);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlate(String licensePlate);

    Optional<Vehicle> findByIdAndOwnerId(Long id, Long ownerId);
}
