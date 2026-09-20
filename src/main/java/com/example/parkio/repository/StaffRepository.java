package com.example.parkio.repository;

import com.example.parkio.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByOrganizationIdAndUserIdAndActiveTrue(Long organizationId, Long userId);

    List<Staff> findByOrganizationIdAndActiveTrue(Long organizationId);

    List<Staff> findByReportsToId(Long reportsToStaffId);

    List<Staff> findByUserIdAndActiveTrue(Long userId);

    boolean existsByOrganizationIdAndUserIdAndActiveTrue(Long organizationId, Long userId);
}
