package com.example.parkio.service;

import com.example.parkio.dto.request.PricingRuleRequest;
import com.example.parkio.dto.response.PricingRuleResponse;
import com.example.parkio.entity.ParkingLot;
import com.example.parkio.entity.ParkingSpot;
import com.example.parkio.entity.PricingRule;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;
    private final ParkingLotService parkingLotService;

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PricingRuleResponse> getByLot(Long lotId) {
        parkingLotService.findById(lotId); // validate lot exists
        return pricingRuleRepository.findByParkingLotIdAndActiveTrue(lotId)
                .stream().map(PricingRuleResponse::from).toList();
    }

    @Transactional
    public PricingRuleResponse create(Long lotId, PricingRuleRequest request) {
        ParkingLot lot = parkingLotService.findById(lotId);
        PricingRule rule = PricingRule.builder()
                .parkingLot(lot)
                .name(request.name())
                .type(request.type())
                .applicableDays(request.applicableDays())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .multiplier(request.multiplier())
                .active(request.active())
                .build();
        return PricingRuleResponse.from(pricingRuleRepository.save(rule));
    }

    @Transactional
    public PricingRuleResponse update(Long ruleId, PricingRuleRequest request) {
        PricingRule rule = pricingRuleRepository.findById(ruleId)
                .orElseThrow(() -> ParkioException.notFound("Pricing rule not found: " + ruleId));
        rule.setName(request.name());
        rule.setType(request.type());
        rule.setApplicableDays(request.applicableDays());
        rule.setStartTime(request.startTime());
        rule.setEndTime(request.endTime());
        rule.setMultiplier(request.multiplier());
        rule.setActive(request.active());
        return PricingRuleResponse.from(pricingRuleRepository.save(rule));
    }

    @Transactional
    public void delete(Long ruleId) {
        if (!pricingRuleRepository.existsById(ruleId)) {
            throw ParkioException.notFound("Pricing rule not found: " + ruleId);
        }
        pricingRuleRepository.deleteById(ruleId);
    }

    // ── Pricing calculation ───────────────────────────────────────────────────

    /**
     * Calculates the total booking cost by splitting the booking window into
     * per-hour slots and applying the highest-priority matching rule to each.
     * Falls back to the lot's base hourly rate when no rule matches.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateAmount(ParkingSpot spot, LocalDateTime start, LocalDateTime end) {
        ParkingLot lot = spot.getParkingLot();
        List<PricingRule> rules = pricingRuleRepository.findByParkingLotIdAndActiveTrue(lot.getId());

        long totalMinutes = Duration.between(start, end).toMinutes();
        BigDecimal totalCost = BigDecimal.ZERO;

        // Walk minute-by-minute in 30-minute slices for accuracy
        LocalDateTime cursor = start;
        while (cursor.isBefore(end)) {
            LocalDateTime sliceEnd = cursor.plusMinutes(30).isAfter(end) ? end : cursor.plusMinutes(30);
            long sliceMinutes = Duration.between(cursor, sliceEnd).toMinutes();

            BigDecimal multiplier = findMultiplier(rules, cursor);
            BigDecimal sliceHours = BigDecimal.valueOf(sliceMinutes)
                    .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
            BigDecimal sliceCost = lot.getHourlyRate()
                    .multiply(multiplier)
                    .multiply(sliceHours);
            totalCost = totalCost.add(sliceCost);
            cursor = sliceEnd;
        }

        return totalCost.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal findMultiplier(List<PricingRule> rules, LocalDateTime dt) {
        DayOfWeek day = dt.getDayOfWeek();
        LocalTime time = dt.toLocalTime();

        return rules.stream()
                .filter(r -> r.getDaysOfWeek().contains(day))
                .filter(r -> {
                    if (r.getStartTime() == null || r.getEndTime() == null) return true;
                    // Handle overnight rules (e.g. 22:00–06:00)
                    if (r.getEndTime().isBefore(r.getStartTime())) {
                        return !time.isBefore(r.getStartTime()) || time.isBefore(r.getEndTime());
                    }
                    return !time.isBefore(r.getStartTime()) && time.isBefore(r.getEndTime());
                })
                .map(PricingRule::getMultiplier)
                .max(BigDecimal::compareTo) // highest multiplier wins if multiple apply
                .orElse(BigDecimal.ONE);
    }
}
