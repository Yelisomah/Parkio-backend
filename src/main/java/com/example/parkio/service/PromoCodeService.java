package com.example.parkio.service;

import com.example.parkio.dto.request.PromoCodeRequest;
import com.example.parkio.dto.response.PromoCodeResponse;
import com.example.parkio.entity.AuditLog;
import com.example.parkio.entity.PromoCode;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Replaces the original DiscountService's hardcoded two-entry table
 * (tasks.md task 10). Redemption is atomic and cap-enforcing — see
 * PromoCodeRepository.tryClaimUse's javadoc for why a plain
 * read-then-write in this service wouldn't be safe under concurrency.
 */
@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final AuditService auditService;

    /**
     * @return the discount amount for the given code, or ZERO if no code was supplied.
     * @throws ParkioException badRequest if the code doesn't exist, or is inactive/expired/at its cap.
     *
     * Called from within BookingService's @Transactional create()/createWalkUp() —
     * if the booking creation later fails and the transaction rolls back, the
     * claimed use rolls back with it (same DB transaction), so a failed
     * booking never permanently burns a redemption.
     */
    @Transactional
    public BigDecimal redeem(String code, BigDecimal totalAmount) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }

        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> ParkioException.badRequest("Invalid promo code: " + code));

        int claimed = promoCodeRepository.tryClaimUse(promo.getId(), LocalDateTime.now());
        if (claimed == 0) {
            throw ParkioException.badRequest(
                    "Promo code is no longer valid (inactive, expired, or usage limit reached): " + code);
        }

        return totalAmount.multiply(promo.getDiscountPercent()).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public PromoCodeResponse create(PromoCodeRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        if (promoCodeRepository.findByCodeIgnoreCase(normalizedCode).isPresent()) {
            throw ParkioException.conflict("Promo code already exists: " + normalizedCode);
        }

        PromoCode promo = PromoCode.builder()
                .code(normalizedCode)
                .discountPercent(request.discountPercent())
                .maxUses(request.maxUses())
                .expiresAt(request.expiresAt())
                .build();
        PromoCode saved = promoCodeRepository.save(promo);

        auditService.log("admin", AuditLog.AuditAction.PROMO_CODE_CREATED,
                "PromoCode", saved.getId(), "Promo code " + normalizedCode + " created (" +
                        request.discountPercent().multiply(BigDecimal.valueOf(100)) + "% off)");

        return PromoCodeResponse.from(saved);
    }

    @Transactional
    public void deactivate(Long id) {
        PromoCode promo = findById(id);
        promo.setActive(false);
        promoCodeRepository.save(promo);

        auditService.log("admin", AuditLog.AuditAction.PROMO_CODE_DEACTIVATED,
                "PromoCode", promo.getId(), "Promo code " + promo.getCode() + " deactivated");
    }

    @Transactional(readOnly = true)
    public List<PromoCodeResponse> listAll() {
        return promoCodeRepository.findAll().stream().map(PromoCodeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PromoCode findById(Long id) {
        return promoCodeRepository.findById(id)
                .orElseThrow(() -> ParkioException.notFound("Promo code not found: " + id));
    }
}
