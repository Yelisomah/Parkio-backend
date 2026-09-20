package com.example.parkio.repository;

import com.example.parkio.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByPhoneAndPurposeOrderByCreatedAtDesc(String phone, Otp.Purpose purpose);
}
