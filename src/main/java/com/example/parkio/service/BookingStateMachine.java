package com.example.parkio.service;

import com.example.parkio.entity.Booking;
import com.example.parkio.exception.ParkioException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Explicit, enum-driven booking state machine — per tech-standards.md:
 * "modeled as an explicit enum-driven state machine, not boolean flags,
 * because three separate entry points (driver app, warden app, QR scan) can
 * all create bookings against the same space."
 *
 * Every status change in BookingService should go through
 * {@link #assertLegalTransition}, not a bare setStatus() call, so an illegal
 * transition (e.g. COMPLETED → CONFIRMED — tech-standards.md's own example)
 * is rejected in exactly one place rather than re-implemented ad hoc per method.
 *
 * The transition map's logic was verified standalone (transition coverage,
 * terminal-state checks, no self-transitions, reachability from PENDING)
 * before being wired in here — see STATUS_AND_ROADMAP.md Phase C notes.
 */
@Component
public class BookingStateMachine {

    private static final Map<Booking.BookingStatus, Set<Booking.BookingStatus>> TRANSITIONS =
            new EnumMap<>(Booking.BookingStatus.class);

    static {
        TRANSITIONS.put(Booking.BookingStatus.PENDING,
                EnumSet.of(Booking.BookingStatus.CONFIRMED, Booking.BookingStatus.CANCELLED));
        TRANSITIONS.put(Booking.BookingStatus.CONFIRMED,
                EnumSet.of(Booking.BookingStatus.ACTIVE, Booking.BookingStatus.CANCELLED, Booking.BookingStatus.NO_SHOW));
        TRANSITIONS.put(Booking.BookingStatus.ACTIVE,
                EnumSet.of(Booking.BookingStatus.COMPLETED, Booking.BookingStatus.OVERSTAYED, Booking.BookingStatus.DISPUTED));
        TRANSITIONS.put(Booking.BookingStatus.OVERSTAYED,
                EnumSet.of(Booking.BookingStatus.COMPLETED, Booking.BookingStatus.DISPUTED));
        TRANSITIONS.put(Booking.BookingStatus.COMPLETED,
                EnumSet.of(Booking.BookingStatus.DISPUTED));
        // Terminal — no legal transitions out
        TRANSITIONS.put(Booking.BookingStatus.CANCELLED, EnumSet.noneOf(Booking.BookingStatus.class));
        TRANSITIONS.put(Booking.BookingStatus.NO_SHOW, EnumSet.noneOf(Booking.BookingStatus.class));
        TRANSITIONS.put(Booking.BookingStatus.DISPUTED, EnumSet.noneOf(Booking.BookingStatus.class));
    }

    public boolean isLegal(Booking.BookingStatus from, Booking.BookingStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /** @throws ParkioException badRequest if the transition isn't in the map. */
    public void assertLegalTransition(Booking.BookingStatus from, Booking.BookingStatus to) {
        if (!isLegal(from, to)) {
            throw ParkioException.badRequest("Illegal booking transition: " + from + " -> " + to);
        }
    }
}
