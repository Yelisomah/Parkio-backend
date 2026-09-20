package com.example.parkio.service;

import com.example.parkio.dto.response.CollectionsSummaryResponse;
import com.example.parkio.entity.*;
import com.example.parkio.repository.BookingRepository;
import com.example.parkio.repository.DailyCollectionsSummaryRepository;
import com.example.parkio.repository.ParkingLotRepository;
import com.example.parkio.repository.PaymentRepository;
import com.example.parkio.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collections reconciliation (tasks.md task 9). See DailyCollectionsSummary's
 * javadoc for how "per space per staff per day" was adapted to this
 * codebase's payment model (digital payments have no staff dimension; only
 * cash-logged ones do).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionsReconciliationService {

    private final ParkingLotRepository parkingLotRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final DailyCollectionsSummaryRepository summaryRepository;
    private final StaffRepository staffRepository;
    private final AuditService auditService;

    /** Default 20% per tasks.md 9.3 ("configurable threshold, default 20%"). */
    @Value("${app.collections.discrepancy-threshold-percent:20}")
    private double discrepancyThresholdPercent;

    /** How many prior days feed the trailing average. */
    @Value("${app.collections.trailing-window-days:14}")
    private int trailingWindowDays;

    /**
     * Aggregates one lot's collections for one calendar day and upserts the
     * space-level (staff = null) summary plus one row per staff member who
     * logged cash that day. Called by CollectionsSchedulerService nightly,
     * but exposed here (not private) so it can be re-run manually for a
     * backfill/correction without waiting for the next scheduled run.
     */
    @Transactional
    public void aggregateForLotAndDate(ParkingLot lot, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Payment> completed = paymentRepository.findCompletedForLotOnDay(lot.getId(), dayStart, dayEnd);

        BigDecimal digitalTotal = BigDecimal.ZERO;
        BigDecimal unattributedCash = BigDecimal.ZERO;
        Map<Staff, BigDecimal> cashByStaff = new HashMap<>();

        for (Payment p : completed) {
            if (p.getPaymentMethod() == Payment.PaymentMethod.CASH) {
                Staff loggingStaff = extractLoggingStaff(p.getGatewayReference());
                if (loggingStaff != null) {
                    cashByStaff.merge(loggingStaff, p.getAmount(), BigDecimal::add);
                } else {
                    // Couldn't resolve which staff member logged it — still count the
                    // cash in the space-level total (never silently drop collected
                    // money) via unattributedCash, just without a per-staff row.
                    unattributedCash = unattributedCash.add(p.getAmount());
                }
            } else {
                digitalTotal = digitalTotal.add(p.getAmount());
            }
        }

        BigDecimal expected = bookingRepository.sumExpectedForLotOnDay(lot.getId(), dayStart, dayEnd);
        BigDecimal staffAttributedCash = cashByStaff.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCash = staffAttributedCash.add(unattributedCash);

        upsertSpaceLevelRow(lot, date, expected, digitalTotal, totalCash);

        for (Map.Entry<Staff, BigDecimal> entry : cashByStaff.entrySet()) {
            upsertStaffCashRow(lot, entry.getKey(), date, entry.getValue());
        }
    }

    private void upsertSpaceLevelRow(ParkingLot lot, LocalDate date, BigDecimal expected, BigDecimal digital, BigDecimal cash) {
        DailyCollectionsSummary row = summaryRepository.findBySpaceIdAndStaffIsNullAndSummaryDate(lot.getId(), date)
                .orElseGet(() -> DailyCollectionsSummary.builder().space(lot).staff(null).summaryDate(date).build());

        row.setExpectedAmount(expected != null ? expected : BigDecimal.ZERO);
        row.setDigitalAmount(digital);
        row.setCashLoggedAmount(cash);

        BigDecimal actualTotal = digital.add(cash);
        List<DailyCollectionsSummary> trailing = summaryRepository.findTrailingForSpace(lot.getId(), date).stream()
                .limit(trailingWindowDays)
                .toList();

        if (trailing.isEmpty()) {
            row.setDiscrepancyFlag(false);
            row.setDiscrepancyNote(null);
        } else {
            BigDecimal trailingAvg = trailing.stream()
                    .map(t -> t.getDigitalAmount().add(t.getCashLoggedAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(trailing.size()), 4, RoundingMode.HALF_UP);

            if (trailingAvg.compareTo(BigDecimal.ZERO) == 0) {
                row.setDiscrepancyFlag(actualTotal.compareTo(BigDecimal.ZERO) > 0);
                row.setDiscrepancyNote(row.isDiscrepancyFlag() ? "Trailing average is zero but today's collections are not" : null);
            } else {
                BigDecimal variancePercent = actualTotal.subtract(trailingAvg).abs()
                        .divide(trailingAvg, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                boolean flagged = variancePercent.doubleValue() > discrepancyThresholdPercent;
                row.setDiscrepancyFlag(flagged);
                row.setDiscrepancyNote(flagged
                        ? String.format("%.1f%% variance from %d-day trailing average (%.2f vs actual %.2f)",
                                variancePercent.doubleValue(), trailing.size(), trailingAvg, actualTotal)
                        : null);

                if (flagged) {
                    auditService.log("system", AuditLog.AuditAction.COLLECTIONS_DISCREPANCY_FLAGGED,
                            "ParkingLot", lot.getId(), row.getDiscrepancyNote());
                }
            }
        }

        summaryRepository.save(row);
    }

    private void upsertStaffCashRow(ParkingLot lot, Staff staff, LocalDate date, BigDecimal cashAmount) {
        DailyCollectionsSummary row = summaryRepository.findBySpaceIdAndStaffIdAndSummaryDate(lot.getId(), staff.getId(), date)
                .orElseGet(() -> DailyCollectionsSummary.builder().space(lot).staff(staff).summaryDate(date).build());
        row.setCashLoggedAmount(cashAmount);
        row.setDigitalAmount(BigDecimal.ZERO); // not attributable to staff — see class javadoc
        row.setExpectedAmount(cashAmount); // best available "expected" at staff granularity is what they logged
        summaryRepository.save(row);
    }

    /**
     * Best-effort: recovers which staff member logged a CASH payment from
     * PaymentService.logCashPayment's gatewayReference convention
     * ("CASH-LOGGED-STAFF-{id}"). A bit fragile — flagged as a follow-up
     * candidate for a proper loggedByStaff FK on Payment instead of
     * string-parsing, if this reconciliation feature gets real usage.
     */
    private Staff extractLoggingStaff(String gatewayReference) {
        if (gatewayReference == null || !gatewayReference.startsWith("CASH-LOGGED-STAFF-")) {
            return null;
        }
        try {
            Long staffId = Long.parseLong(gatewayReference.substring("CASH-LOGGED-STAFF-".length()));
            return staffRepository.findById(staffId).orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<CollectionsSummaryResponse> getFlagged(Long spaceId) {
        List<DailyCollectionsSummary> rows = spaceId != null
                ? summaryRepository.findBySpaceIdAndDiscrepancyFlagTrueOrderBySummaryDateDesc(spaceId)
                : summaryRepository.findByDiscrepancyFlagTrueOrderBySummaryDateDesc();
        return rows.stream().map(CollectionsSummaryResponse::from).toList();
    }

    @Transactional
    public void aggregateAllLotsForDate(LocalDate date) {
        for (ParkingLot lot : parkingLotRepository.findAll()) {
            try {
                aggregateForLotAndDate(lot, date);
            } catch (Exception ex) {
                log.error("Collections aggregation failed for lot {} on {}", lot.getId(), date, ex);
            }
        }
    }
}
