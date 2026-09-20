package com.example.parkio.repository;

import com.example.parkio.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    List<PricingRule> findByParkingLotIdAndActiveTrue(Long lotId);

    void deleteByParkingLotId(Long lotId);
}
