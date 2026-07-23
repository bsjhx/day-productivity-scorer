package com.bsjhx.dayproductivityscore.infrastructure.query;

import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.DayScore;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayLocked;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayRated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class DayProjectionListenerIntegrationTest {

    @Autowired
    private DayProjectionJdbcRepository repository;

    @Autowired
    private DayProjectionListener listener;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldCreateNewProjectionWhenDayRatedEventReceived() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 23));
        DayRated event = new DayRated(dayId, DayScore.FIVE);

        // when
        listener.on(event);

        // then
        Optional<DayProjection> result = repository.findById(dayId.id());
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getScore());
        assertFalse(result.get().isLocked());
    }

    @Test
    void shouldUpdateExistingProjectionScore() {
        // given
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        // Create initial projection
        listener.on(new DayRated(dayId, DayScore.TWO));

        // when - update score
        listener.on(new DayRated(dayId, DayScore.FIVE));

        // then
        Optional<DayProjection> result = repository.findById(date);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getScore());
        assertFalse(result.get().isLocked());
    }

    @Test
    void shouldLockExistingProjection() {
        // given
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        // Create and rate day
        listener.on(new DayRated(dayId, DayScore.FOUR));

        // when - lock day
        listener.on(new DayLocked(dayId));

        // then
        Optional<DayProjection> result = repository.findById(date);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getScore());
        assertTrue(result.get().isLocked());
    }

    @Test
    void shouldThrowExceptionWhenLockingNonExistentDay() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 23));
        DayLocked event = new DayLocked(dayId);

        // when & then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> listener.on(event)
        );

        assertEquals("DayLocked event received for a day that does not exist in the read model",
            exception.getMessage());
    }

    @Test
    void shouldHandleMultipleRatingsBeforeLocking() {
        // given
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        // when - rate multiple times then lock
        listener.on(new DayRated(dayId, DayScore.ONE));
        listener.on(new DayRated(dayId, DayScore.THREE));
        listener.on(new DayRated(dayId, DayScore.FIVE));
        listener.on(new DayLocked(dayId));

        // then
        Optional<DayProjection> result = repository.findById(date);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getScore());
        assertTrue(result.get().isLocked());
    }

    @Test
    void shouldUpdateScoreEvenWhenAlreadyLocked() {
        // given
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        listener.on(new DayRated(dayId, DayScore.TWO));
        listener.on(new DayLocked(dayId));

        // when - update score after locking
        listener.on(new DayRated(dayId, DayScore.FIVE));

        // then
        Optional<DayProjection> result = repository.findById(date);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getScore());
        assertTrue(result.get().isLocked()); // Still locked
    }

    @Test
    void shouldHandleMultipleDaysIndependently() {
        // given
        LocalDate date1 = LocalDate.of(2026, 7, 20);
        LocalDate date2 = LocalDate.of(2026, 7, 21);
        LocalDate date3 = LocalDate.of(2026, 7, 22);

        DayId dayId1 = DayId.of(date1);
        DayId dayId2 = DayId.of(date2);
        DayId dayId3 = DayId.of(date3);

        // when
        listener.on(new DayRated(dayId1, DayScore.TWO));
        listener.on(new DayRated(dayId2, DayScore.FOUR));
        listener.on(new DayRated(dayId3, DayScore.FIVE));
        listener.on(new DayLocked(dayId2));

        // then
        Optional<DayProjection> result1 = repository.findById(date1);
        Optional<DayProjection> result2 = repository.findById(date2);
        Optional<DayProjection> result3 = repository.findById(date3);

        assertTrue(result1.isPresent());
        assertEquals(2, result1.get().getScore());
        assertFalse(result1.get().isLocked());

        assertTrue(result2.isPresent());
        assertEquals(4, result2.get().getScore());
        assertTrue(result2.get().isLocked());

        assertTrue(result3.isPresent());
        assertEquals(5, result3.get().getScore());
        assertFalse(result3.get().isLocked());
    }

    @Test
    void shouldHandleAllDayScoreValues() {
        // given
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);
        DayScore[] allScores = {DayScore.NONE, DayScore.ZERO, DayScore.ONE,
                                DayScore.TWO, DayScore.THREE, DayScore.FOUR, DayScore.FIVE};

        // when & then
        for (DayScore score : allScores) {
            listener.on(new DayRated(dayId, score));

            Optional<DayProjection> result = repository.findById(date);
            assertTrue(result.isPresent());
            assertEquals(score.getScore(), result.get().getScore(),
                "Failed to handle score: " + score);
        }
    }

    @Test
    void shouldPersistDataAcrossTransactions() {
        // given
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        listener.on(new DayRated(dayId, DayScore.THREE));

        // when - verify data persists
        Optional<DayProjection> result1 = repository.findById(date);

        // Update in same transaction
        listener.on(new DayRated(dayId, DayScore.FIVE));
        Optional<DayProjection> result2 = repository.findById(date);

        // then
        assertTrue(result1.isPresent());
        assertEquals(3, result1.get().getScore());

        assertTrue(result2.isPresent());
        assertEquals(5, result2.get().getScore());
    }

    @Test
    void shouldHandleCompleteWorkflow() {
        // given
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        // when - complete workflow: rate → update → update → lock
        listener.on(new DayRated(dayId, DayScore.ONE));
        Optional<DayProjection> afterFirstRating = repository.findById(date);

        listener.on(new DayRated(dayId, DayScore.THREE));
        Optional<DayProjection> afterSecondRating = repository.findById(date);

        listener.on(new DayRated(dayId, DayScore.FIVE));
        Optional<DayProjection> afterThirdRating = repository.findById(date);

        listener.on(new DayLocked(dayId));
        Optional<DayProjection> afterLocking = repository.findById(date);

        // then - verify each step
        assertTrue(afterFirstRating.isPresent());
        assertEquals(1, afterFirstRating.get().getScore());
        assertFalse(afterFirstRating.get().isLocked());

        assertTrue(afterSecondRating.isPresent());
        assertEquals(3, afterSecondRating.get().getScore());
        assertFalse(afterSecondRating.get().isLocked());

        assertTrue(afterThirdRating.isPresent());
        assertEquals(5, afterThirdRating.get().getScore());
        assertFalse(afterThirdRating.get().isLocked());

        assertTrue(afterLocking.isPresent());
        assertEquals(5, afterLocking.get().getScore());
        assertTrue(afterLocking.get().isLocked());
    }

    @Test
    void shouldNotCreateDuplicateProjections() {
        // given
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        // when - rate same day multiple times
        listener.on(new DayRated(dayId, DayScore.TWO));
        listener.on(new DayRated(dayId, DayScore.THREE));
        listener.on(new DayRated(dayId, DayScore.FOUR));

        // then - verify only one projection exists
        long count = repository.count();
        assertEquals(1, count);

        Optional<DayProjection> result = repository.findById(date);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getScore());
    }

    @Test
    void shouldHandleBoundaryDates() {
        // given - test with different date boundaries
        LocalDate startOfYear = LocalDate.of(2026, 1, 1);
        LocalDate endOfYear = LocalDate.of(2026, 12, 31);
        LocalDate leapDay = LocalDate.of(2024, 2, 29);

        DayId dayId1 = DayId.of(startOfYear);
        DayId dayId2 = DayId.of(endOfYear);
        DayId dayId3 = DayId.of(leapDay);

        // when
        listener.on(new DayRated(dayId1, DayScore.ONE));
        listener.on(new DayRated(dayId2, DayScore.THREE));
        listener.on(new DayRated(dayId3, DayScore.FIVE));

        // then
        assertTrue(repository.findById(startOfYear).isPresent());
        assertTrue(repository.findById(endOfYear).isPresent());
        assertTrue(repository.findById(leapDay).isPresent());

        assertEquals(3, repository.count());
    }
}
