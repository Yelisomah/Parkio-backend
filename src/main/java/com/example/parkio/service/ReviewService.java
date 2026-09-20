package com.example.parkio.service;

import com.example.parkio.dto.request.ReviewRequest;
import com.example.parkio.dto.response.PageResponse;
import com.example.parkio.dto.response.ReviewResponse;
import com.example.parkio.entity.Booking;
import com.example.parkio.entity.Review;
import com.example.parkio.exception.ParkioException;
import com.example.parkio.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingService bookingService;
    private final UserService userService;

    @Transactional
    public ReviewResponse create(Long userId, ReviewRequest request) {
        if (reviewRepository.existsByBookingId(request.bookingId())) {
            throw ParkioException.conflict("A review already exists for this booking");
        }

        Booking booking = bookingService.findById(request.bookingId());

        if (!booking.getUser().getId().equals(userId)) {
            throw ParkioException.forbidden("You can only review your own bookings");
        }
        if (booking.getStatus() != Booking.BookingStatus.COMPLETED) {
            throw ParkioException.badRequest("You can only review completed bookings");
        }

        Review review = Review.builder()
                .booking(booking)
                .user(userService.findById(userId))
                .parkingLot(booking.getSpot().getParkingLot())
                .rating(request.rating())
                .comment(request.comment())
                .build();

        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getByLot(Long lotId, Pageable pageable) {
        return PageResponse.from(
                reviewRepository.findByParkingLotIdAndVisibleTrue(lotId, pageable)
                        .map(ReviewResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getByUser(Long userId, Pageable pageable) {
        return PageResponse.from(
                reviewRepository.findByUserId(userId, pageable)
                        .map(ReviewResponse::from));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLotStats(Long lotId) {
        Double avg = reviewRepository.findAverageRatingByLotId(lotId);
        long count = reviewRepository.countByLotId(lotId);
        return Map.of(
                "averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0,
                "reviewCount", count
        );
    }

    @Transactional
    public void toggleVisibility(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ParkioException.notFound("Review not found: " + reviewId));
        review.setVisible(!review.isVisible());
        reviewRepository.save(review);
    }

    @Transactional
    public void delete(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw ParkioException.notFound("Review not found: " + reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }
}
