package com.bsjhx.dayproductivityscore.domain;

import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayLocked;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayRated;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DayAggregateTest {

    @Test
    void shouldCreateNewDayAggregateWithDefaultValues() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));

        // when
        DayAggregate aggregate = new DayAggregate(dayId);

        // then
        assertEquals(dayId, aggregate.getId());
        assertEquals(DayScore.NONE, aggregate.getDayScore());
        assertFalse(aggregate.isLocked());
        assertEquals(0, aggregate.getExpectedVersion());
        assertTrue(aggregate.getChanges().isEmpty());
    }

    @Test
    void shouldRateDayAndRecordEvent() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);

        // when
        aggregate.rate(DayScore.FIVE);

        // then
        assertEquals(DayScore.FIVE, aggregate.getDayScore());
        assertEquals(1, aggregate.getChanges().size());

        DayDomainEvent event = aggregate.getChanges().get(0);
        assertInstanceOf(DayRated.class, event);
        DayRated dayRated = (DayRated) event;
        assertEquals(dayId, dayRated.dayId());
        assertEquals(DayScore.FIVE, dayRated.score());
    }

    @Test
    void shouldAllowRatingDayMultipleTimes() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);

        // when
        aggregate.rate(DayScore.THREE);
        aggregate.rate(DayScore.FIVE);

        // then
        assertEquals(DayScore.FIVE, aggregate.getDayScore());
        assertEquals(2, aggregate.getChanges().size());
    }

    @Test
    void shouldThrowExceptionWhenRatingNullScore() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aggregate.rate(null)
        );
        assertEquals("DayScore cannot be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRatingFutureDay() {
        // given
        DayId futureDay = DayId.of(LocalDate.now().plusDays(1));
        DayAggregate aggregate = new DayAggregate(futureDay);

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aggregate.rate(DayScore.FIVE)
        );
        assertEquals("Must not rate a day in the future", exception.getMessage());
    }

    @Test
    void shouldAllowRatingToday() {
        // given
        DayId today = DayId.of(LocalDate.now());
        DayAggregate aggregate = new DayAggregate(today);

        // when
        aggregate.rate(DayScore.FOUR);

        // then
        assertEquals(DayScore.FOUR, aggregate.getDayScore());
        assertEquals(1, aggregate.getChanges().size());
    }

    @Test
    void shouldLockDayAndRecordEvent() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);

        // when
        aggregate.lock();

        // then
        assertTrue(aggregate.isLocked());
        assertEquals(1, aggregate.getChanges().size());

        DayDomainEvent event = aggregate.getChanges().get(0);
        assertInstanceOf(DayLocked.class, event);
        DayLocked dayLocked = (DayLocked) event;
        assertEquals(dayId, dayLocked.dayId());
    }

    @Test
    void shouldNotRecordMultipleLockEvents() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);

        // when
        aggregate.lock();
        aggregate.lock();
        aggregate.lock();

        // then
        assertTrue(aggregate.isLocked());
        assertEquals(1, aggregate.getChanges().size());
    }

    @Test
    void shouldThrowExceptionWhenRatingLockedDay() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.THREE);
        aggregate.lock();

        // when & then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> aggregate.rate(DayScore.FIVE)
        );
        assertEquals("DayScore cannot be changed when the day is locked", exception.getMessage());
    }

    @Test
    void shouldClearChanges() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.FOUR);
        aggregate.lock();

        // when
        aggregate.clearChanges();

        // then
        assertTrue(aggregate.getChanges().isEmpty());
        assertEquals(DayScore.FOUR, aggregate.getDayScore());
        assertTrue(aggregate.isLocked());
    }

    @Test
    void shouldReturnUnmodifiableListOfChanges() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.THREE);

        // when
        List<DayDomainEvent> changes = aggregate.getChanges();

        // then
        assertThrows(UnsupportedOperationException.class, () ->
            changes.add(new DayRated(dayId, DayScore.FIVE))
        );
    }

    @Test
    void shouldRecreateAggregateFromEmptyHistory() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        List<DayDomainEvent> history = List.of();

        // when
        DayAggregate aggregate = DayAggregate.recreate(dayId, history);

        // then
        assertEquals(dayId, aggregate.getId());
        assertEquals(DayScore.NONE, aggregate.getDayScore());
        assertFalse(aggregate.isLocked());
        assertEquals(0, aggregate.getExpectedVersion());
        assertTrue(aggregate.getChanges().isEmpty());
    }

    @Test
    void shouldRecreateAggregateFromSingleRatingEvent() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        List<DayDomainEvent> history = List.of(
            new DayRated(dayId, DayScore.FOUR)
        );

        // when
        DayAggregate aggregate = DayAggregate.recreate(dayId, history);

        // then
        assertEquals(dayId, aggregate.getId());
        assertEquals(DayScore.FOUR, aggregate.getDayScore());
        assertFalse(aggregate.isLocked());
        assertEquals(1, aggregate.getExpectedVersion());
        assertTrue(aggregate.getChanges().isEmpty());
    }

    @Test
    void shouldRecreateAggregateFromMultipleEvents() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        List<DayDomainEvent> history = List.of(
            new DayRated(dayId, DayScore.TWO),
            new DayRated(dayId, DayScore.FOUR),
            new DayLocked(dayId)
        );

        // when
        DayAggregate aggregate = DayAggregate.recreate(dayId, history);

        // then
        assertEquals(dayId, aggregate.getId());
        assertEquals(DayScore.FOUR, aggregate.getDayScore());
        assertTrue(aggregate.isLocked());
        assertEquals(3, aggregate.getExpectedVersion());
        assertTrue(aggregate.getChanges().isEmpty());
    }

    @Test
    void shouldRecreateAggregateWithMultipleRatings() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        List<DayDomainEvent> history = List.of(
            new DayRated(dayId, DayScore.ONE),
            new DayRated(dayId, DayScore.TWO),
            new DayRated(dayId, DayScore.THREE),
            new DayRated(dayId, DayScore.FOUR)
        );

        // when
        DayAggregate aggregate = DayAggregate.recreate(dayId, history);

        // then
        assertEquals(DayScore.FOUR, aggregate.getDayScore());
        assertEquals(4, aggregate.getExpectedVersion());
    }

    @Test
    void shouldAllowNewRatingsAfterRecreation() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        List<DayDomainEvent> history = List.of(
            new DayRated(dayId, DayScore.THREE)
        );
        DayAggregate aggregate = DayAggregate.recreate(dayId, history);

        // when
        aggregate.rate(DayScore.FIVE);

        // then
        assertEquals(DayScore.FIVE, aggregate.getDayScore());
        assertEquals(1, aggregate.getChanges().size());
        assertEquals(1, aggregate.getExpectedVersion());
    }

    @Test
    void shouldNotAllowRatingAfterRecreatingLockedDay() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        List<DayDomainEvent> history = List.of(
            new DayRated(dayId, DayScore.THREE),
            new DayLocked(dayId)
        );
        DayAggregate aggregate = DayAggregate.recreate(dayId, history);

        // when & then
        assertThrows(IllegalStateException.class, () ->
            aggregate.rate(DayScore.FIVE)
        );
    }

    @Test
    void shouldHandleCompleteWorkflow() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);

        // when - rate the day multiple times, then lock
        aggregate.rate(DayScore.TWO);
        aggregate.rate(DayScore.THREE);
        aggregate.rate(DayScore.FOUR);
        aggregate.lock();

        // then
        assertEquals(DayScore.FOUR, aggregate.getDayScore());
        assertTrue(aggregate.isLocked());
        assertEquals(4, aggregate.getChanges().size());

        // verify events in order
        List<DayDomainEvent> changes = aggregate.getChanges();
        assertInstanceOf(DayRated.class, changes.get(0));
        assertInstanceOf(DayRated.class, changes.get(1));
        assertInstanceOf(DayRated.class, changes.get(2));
        assertInstanceOf(DayLocked.class, changes.get(3));

        // verify cannot rate after lock
        assertThrows(IllegalStateException.class, () ->
            aggregate.rate(DayScore.FIVE)
        );
    }
}
