package com.example.parkio.service;

import com.example.parkio.entity.Booking;
import com.example.parkio.exception.ParkioException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests — no Spring context needed, BookingStateMachine has no
 * dependencies beyond the Booking.BookingStatus enum. The transition map's
 * core logic was also verified standalone (outside the Spring build) before
 * this class was written — see STATUS_AND_ROADMAP.md Phase C notes.
 */
class BookingStateMachineTest {

    private final BookingStateMachine stateMachine = new BookingStateMachine();

    @Test
    void rejectsCheckedOutToConfirmed() {
        // tech-standards.md's own explicit example of an illegal transition.
        assertThat(stateMachine.isLegal(Booking.BookingStatus.COMPLETED, Booking.BookingStatus.CONFIRMED)).isFalse();

        assertThatThrownBy(() -> stateMachine.assertLegalTransition(
                Booking.BookingStatus.COMPLETED, Booking.BookingStatus.CONFIRMED))
                .isInstanceOf(ParkioException.class);
    }

    @Test
    void rejectsTransitionsOutOfTerminalStates() {
        for (Booking.BookingStatus terminal : new Booking.BookingStatus[]{
                Booking.BookingStatus.CANCELLED, Booking.BookingStatus.NO_SHOW, Booking.BookingStatus.DISPUTED}) {
            for (Booking.BookingStatus target : Booking.BookingStatus.values()) {
                assertThat(stateMachine.isLegal(terminal, target))
                        .as(terminal + " -> " + target + " should be illegal (terminal state)")
                        .isFalse();
            }
        }
    }

    @Test
    void allowsTheHappyPath() {
        assertThat(stateMachine.isLegal(Booking.BookingStatus.PENDING, Booking.BookingStatus.CONFIRMED)).isTrue();
        assertThat(stateMachine.isLegal(Booking.BookingStatus.CONFIRMED, Booking.BookingStatus.ACTIVE)).isTrue();
        assertThat(stateMachine.isLegal(Booking.BookingStatus.ACTIVE, Booking.BookingStatus.COMPLETED)).isTrue();
    }

    @Test
    void rejectsSelfTransitions() {
        for (Booking.BookingStatus s : Booking.BookingStatus.values()) {
            assertThat(stateMachine.isLegal(s, s)).as(s + " -> " + s).isFalse();
        }
    }
}
