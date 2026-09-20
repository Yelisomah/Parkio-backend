package com.example.parkio.repository;

import com.example.parkio.entity.SpaceQrCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpaceQrCodeRepository extends JpaRepository<SpaceQrCode, Long> {
    Optional<SpaceQrCode> findByCode(String code);
    Optional<SpaceQrCode> findBySpaceId(Long spaceId);
    boolean existsBySpaceId(Long spaceId);
}
