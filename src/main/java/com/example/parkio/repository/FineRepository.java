package com.example.parkio.repository;

import com.example.parkio.entity.Fine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FineRepository extends JpaRepository<Fine, Long> {
    List<Fine> findByPlateNumberOrderByIssuedAtDesc(String plateNumber);
    List<Fine> findByPlateNumberAndPaidFalse(String plateNumber);
    List<Fine> findByDisputedTrueOrderByIssuedAtDesc();
}
