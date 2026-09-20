package com.example.parkio.repository;

import com.example.parkio.entity.StaffAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {

    List<StaffAssignment> findByStaffIdAndActiveTrue(Long staffId);

    List<StaffAssignment> findBySpaceIdAndActiveTrue(Long spaceId);

    Optional<StaffAssignment> findByStaffIdAndSpaceIdAndActiveTrue(Long staffId, Long spaceId);

    boolean existsByStaffIdAndSpaceIdAndActiveTrue(Long staffId, Long spaceId);
}
