package com.bsjhx.dayproductivityscore.domain;

import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayLocked;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayRated;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DayAggregateTest {

    @Test
    void shouldCreateNewDayAggregateWithDefaultValues() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // when
        DayAggregate aggregate = DayAggregate.create(id, dayId.id(), userId);

        // then
        assertEquals(id, aggregate.getId());
        assertEquals(userId, aggregate.getUserId());
        assertEquals(DayScore.NONE, aggregate.getDayScore());
        assertFalse(aggregate.isLocked());
        assertEquals(0, aggregate.getVersion());
        assertEquals(1, aggregate.getChanges().size());
    }

    @Test
    void shouldRateDayAndRecordEvent() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = DayAggregate.create(UUID.randomUUID(), dayId.id(), UUID.randomUUID());
        aggregate.clearChanges();

        // when
        aggregate.rate(DayScore.FIVE);

        // then
        assertEquals(DayScore.FIVE, aggregate.getDayScore());
        assertEquals(1, aggregate.getChanges().size());

        DayDomainEvent event = aggregate.getChanges().getFirst();
        assertInstanceOf(DayRated.class, event);
        DayRated dayRated = (DayRated) event;
        assertEquals(dayId, dayRated.dayId());
        assertEquals(DayScore.FIVE, dayRated.score());
    }

    @Test
    void shouldAllowRatingDayMultipleTimes() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = DayAggregate.create(UUID.randomUUID(), dayId.id(), UUID.randomUUID());
        aggregate.clearChanges();

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
        DayAggregate aggregate = DayAggregate.create(UUID.randomUUID(), dayId.id(), UUID.randomUUID());

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
        DayAggregate aggregate = DayAggregate.create(UUID.randomUUID(), futureDay.id(), UUID.randomUUID());

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
        DayAggregate aggregate = DayAggregate.create(UUID.randomUUID(), today.id(), UUID.randomUUID());
        aggregate.clearChanges();

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
        DayAggregate aggregate = DayAggregate.create(UUID.randomUUID(), dayId.id(), UUID.randomUUID());
        aggregate.clearChanges();

        // when
        aggregate.lock();

        // then
        assertTrue(aggregate.isLocked());
        assertEquals(1, aggregate.getChanges().size());

        DayDomainEvent event = aggregate.getChanges().getFirst();
        assertInstanceOf(DayLocked.class, event);
        DayLocked dayLocked = (DayLocked) event;
        assertEquals(dayId, dayLocked.dayId());
    }

    @Test
    void shouldNotRecordMultipleLockEvents() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = DayAggregate.create(UUID.randomUUID(), dayId.id(), UUID.randomUUID());
        aggregate.clearChanges();

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
        DayAggregate aggregate = DayAggregate.create(UUID.randomUUID(), dayId.id(), UUID.randomUUID());
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
        DayAggregate aggregate = DayAggregate.create(UUID.randomUUID(), dayId.id(), UUID.randomUUID());
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
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = DayAggregate.create(id, dayId.id(), userId);
        aggregate.rate(DayScore.THREE);

        // when
        List<DayDomainEvent> changes = aggregate.getChanges();

        // then
        assertThrows(UnsupportedOperationException.class, () ->
            changes.add(new DayRated(id, dayId, DayScore.FIVE, userId))
        );
    }

    @Test
    void shouldRecreateAggregateFromEmptyHistory() {
        // given
        List<DayDomainEvent> history = List.of();

        // when
        DayAggregate aggregate = DayAggregate.recreate(history);

        // then
        assertEquals(DayScore.NONE, aggregate.getDayScore());
        assertFalse(aggregate.isLocked());
        assertEquals(0, aggregate.getVersion());
        assertTrue(aggregate.getChanges().isEmpty());
    }

    @Test
    void shouldRecreateAggregateFromSingleRatingEvent() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        List<DayDomainEvent> history = List.of(
            new DayDomainEvent.DayCreated(id, dayId, userId),
            new DayRated(id, dayId, DayScore.FOUR, userId)
        );

        // when
        DayAggregate aggregate = DayAggregate.recreate(history);

        // then
        assertEquals(id, aggregate.getId());
        assertEquals(userId, aggregate.getUserId());
        assertEquals(DayScore.FOUR, aggregate.getDayScore());
        assertFalse(aggregate.isLocked());
        assertEquals(2, aggregate.getVersion());
        assertTrue(aggregate.getChanges().isEmpty());
    }

    @Test
    void shouldRecreateAggregateFromMultipleEvents() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        List<DayDomainEvent> history = List.of(
            new DayDomainEvent.DayCreated(id, dayId, userId),
            new DayRated(id, dayId, DayScore.TWO, userId),
            new DayRated(id, dayId, DayScore.FOUR, userId),
            new DayLocked(id, dayId)
        );

        // when
        DayAggregate aggregate = DayAggregate.recreate(history);

        // then
        assertEquals(id, aggregate.getId());
        assertEquals(userId, aggregate.getUserId());
        assertEquals(DayScore.FOUR, aggregate.getDayScore());
        assertTrue(aggregate.isLocked());
        assertEquals(4, aggregate.getVersion());
        assertTrue(aggregate.getChanges().isEmpty());
    }

    @Test
    void shouldRecreateAggregateWithMultipleRatings() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        List<DayDomainEvent> history = List.of(
            new DayDomainEvent.DayCreated(id, dayId, userId),
            new DayRated(id, dayId, DayScore.ONE, userId),
            new DayRated(id, dayId, DayScore.TWO, userId),
            new DayRated(id, dayId, DayScore.THREE, userId),
            new DayRated(id, dayId, DayScore.FOUR, userId)
        );

        // when
        DayAggregate aggregate = DayAggregate.recreate(history);

        // then
        assertEquals(DayScore.FOUR, aggregate.getDayScore());
        assertEquals(5, aggregate.getVersion());
    }

    @Test
    void shouldAllowNewRatingsAfterRecreation() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        List<DayDomainEvent> history = List.of(
            new DayDomainEvent.DayCreated(id, dayId, userId),
            new DayRated(id, dayId, DayScore.THREE, userId)
        );
        DayAggregate aggregate = DayAggregate.recreate(history);

        // when
        aggregate.rate(DayScore.FIVE);

        // then
        assertEquals(DayScore.FIVE, aggregate.getDayScore());
        assertEquals(1, aggregate.getChanges().size());
        assertEquals(2, aggregate.getVersion());
    }

    @Test
    void shouldNotAllowRatingAfterRecreatingLockedDay() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        List<DayDomainEvent> history = List.of(
            new DayDomainEvent.DayCreated(id, dayId, userId),
            new DayRated(id, dayId, DayScore.THREE, userId),
            new DayLocked(id, dayId)
        );
        DayAggregate aggregate = DayAggregate.recreate(history);

        // when & then
        assertThrows(IllegalStateException.class, () ->
            aggregate.rate(DayScore.FIVE)
        );
    }

    @Test
    void shouldHandleCompleteWorkflow() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = DayAggregate.create(UUID.randomUUID(), dayId.id(), UUID.randomUUID());
        aggregate.clearChanges();

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
