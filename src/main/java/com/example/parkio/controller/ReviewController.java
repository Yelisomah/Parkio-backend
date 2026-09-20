package com.example.parkio.controller;

import com.example.parkio.dto.request.ReviewRequest;
import com.example.parkio.dto.response.ApiResponse;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.dto.response.ReviewResponse;
import com.example.parkio.service.ReviewService;
import com.example.parkio.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final SecurityUtils securityUtils;

    /** Public — reviews for a parking lot */
    @GetMapping("/parking-lots/{lotId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getByLot(
            @PathVariable Long lotId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getByLot(lotId, pageable)));
    }

    /** Public — average rating stats for a lot */
    @GetMapping("/parking-lots/{lotId}/reviews/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLotStats(@PathVariable Long lotId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getLotStats(lotId)));
    }

    /** Get own reviews */
    @GetMapping("/reviews/me")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMyReviews(
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getByUser(userId, pageable)));
    }

    /** Submit a review for a completed booking */
    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @Valid @RequestBody ReviewRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        ReviewResponse response = reviewService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    /** Admin — toggle review visibility */
    @PatchMapping("/admin/reviews/{id}/toggle-visibility")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleVisibility(@PathVariable Long id) {
        reviewService.toggleVisibility(id);
        return ResponseEntity.ok(ApiResponse.ok("Review visibility toggled"));
    }

    /** Admin — hard delete a review */
    @DeleteMapping("/admin/reviews/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Review deleted"));
    }
}
