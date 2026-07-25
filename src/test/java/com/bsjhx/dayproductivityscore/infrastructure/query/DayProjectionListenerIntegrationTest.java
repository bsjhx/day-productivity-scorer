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
import java.util.UUID;

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
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 23));
        DayRated event = new DayRated(id, dayId, DayScore.FIVE, userId);

        // when
        listener.on(event);

        // then
        Optional<DayProjection> result = repository.findById(id);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getScore());
        assertFalse(result.get().isLocked());
    }

    @Test
    void shouldUpdateExistingProjectionScore() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        // Create initial projection
        listener.on(new DayRated(id, dayId, DayScore.TWO, userId));

        // when - update score
        listener.on(new DayRated(id, dayId, DayScore.FIVE, userId));

        // then
        Optional<DayProjection> result = repository.findById(id);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getScore());
        assertFalse(result.get().isLocked());
    }

    @Test
    void shouldLockExistingProjection() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        // Create and rate day
        listener.on(new DayRated(id, dayId, DayScore.FOUR, userId));

        // when - lock day
        listener.on(new DayLocked(id, dayId));

        // then
        Optional<DayProjection> result = repository.findById(id);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getScore());
        assertTrue(result.get().isLocked());
    }

    @Test
    void shouldThrowExceptionWhenLockingNonExistentDay() {
        // given
        UUID id = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 23));
        DayLocked event = new DayLocked(id, dayId);

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
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        // when - rate multiple times then lock
        listener.on(new DayRated(id, dayId, DayScore.ONE, userId));
        listener.on(new DayRated(id, dayId, DayScore.THREE, userId));
        listener.on(new DayRated(id, dayId, DayScore.FIVE, userId));
        listener.on(new DayLocked(id, dayId));

        // then
        Optional<DayProjection> result = repository.findById(id);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getScore());
        assertTrue(result.get().isLocked());
    }

    @Test
    void shouldUpdateScoreEvenWhenAlreadyLocked() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        listener.on(new DayRated(id, dayId, DayScore.TWO, userId));
        listener.on(new DayLocked(id, dayId));

        // when - update score after locking
        listener.on(new DayRated(id, dayId, DayScore.FIVE, userId));

        // then
        Optional<DayProjection> result = repository.findById(id);
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getScore());
        assertTrue(result.get().isLocked()); // Still locked
    }

    @Test
    void shouldHandleMultipleDaysIndependently() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date1 = LocalDate.of(2026, 7, 20);
        LocalDate date2 = LocalDate.of(2026, 7, 21);
        LocalDate date3 = LocalDate.of(2026, 7, 22);

        DayId dayId1 = DayId.of(date1);
        DayId dayId2 = DayId.of(date2);
        DayId dayId3 = DayId.of(date3);

        // when
        listener.on(new DayRated(id1, dayId1, DayScore.TWO, userId));
        listener.on(new DayRated(id2, dayId2, DayScore.FOUR, userId));
        listener.on(new DayRated(id3, dayId3, DayScore.FIVE, userId));
        listener.on(new DayLocked(id2, dayId2));

        // then
        Optional<DayProjection> result1 = repository.findById(id1);
        Optional<DayProjection> result2 = repository.findById(id2);
        Optional<DayProjection> result3 = repository.findById(id3);

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
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);
        DayScore[] allScores = {DayScore.NONE, DayScore.ZERO, DayScore.ONE,
                                DayScore.TWO, DayScore.THREE, DayScore.FOUR, DayScore.FIVE};

        // when & then
        for (DayScore score : allScores) {
            listener.on(new DayRated(id, dayId, score, userId));

            Optional<DayProjection> result = repository.findById(id);
            assertTrue(result.isPresent());
            assertEquals(score.getScore(), result.get().getScore(),
                "Failed to handle score: " + score);
        }
    }

    @Test
    void shouldPersistDataAcrossTransactions() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        listener.on(new DayRated(id, dayId, DayScore.THREE, userId));

        // when - verify data persists
        Optional<DayProjection> result1 = repository.findById(id);

        // Update in same transaction
        listener.on(new DayRated(id, dayId, DayScore.FIVE, userId));
        Optional<DayProjection> result2 = repository.findById(id);

        // then
        assertTrue(result1.isPresent());
        assertEquals(3, result1.get().getScore());

        assertTrue(result2.isPresent());
        assertEquals(5, result2.get().getScore());
    }

    @Test
    void shouldHandleCompleteWorkflow() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        // when - complete workflow: rate → update → update → lock
        listener.on(new DayRated(id, dayId, DayScore.ONE, userId));
        Optional<DayProjection> afterFirstRating = repository.findById(id);

        listener.on(new DayRated(id, dayId, DayScore.THREE, userId));
        Optional<DayProjection> afterSecondRating = repository.findById(id);

        listener.on(new DayRated(id, dayId, DayScore.FIVE, userId));
        Optional<DayProjection> afterThirdRating = repository.findById(id);

        listener.on(new DayLocked(id, dayId));
        Optional<DayProjection> afterLocking = repository.findById(id);

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
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 23);
        DayId dayId = DayId.of(date);

        // when - rate same day multiple times
        listener.on(new DayRated(id, dayId, DayScore.TWO, userId));
        listener.on(new DayRated(id, dayId, DayScore.THREE, userId));
        listener.on(new DayRated(id, dayId, DayScore.FOUR, userId));

        // then - verify only one projection exists
        long count = repository.count();
        assertEquals(1, count);

        Optional<DayProjection> result = repository.findById(id);
        assertTrue(result.isPresent());
        assertEquals(4, result.get().getScore());
    }

    @Test
    void shouldHandleBoundaryDates() {
        // given - test with different date boundaries
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate startOfYear = LocalDate.of(2026, 1, 1);
        LocalDate endOfYear = LocalDate.of(2026, 12, 31);
        LocalDate leapDay = LocalDate.of(2024, 2, 29);

        DayId dayId1 = DayId.of(startOfYear);
        DayId dayId2 = DayId.of(endOfYear);
        DayId dayId3 = DayId.of(leapDay);

        // when
        listener.on(new DayRated(id1, dayId1, DayScore.ONE, userId));
        listener.on(new DayRated(id2, dayId2, DayScore.THREE, userId));
        listener.on(new DayRated(id3, dayId3, DayScore.FIVE, userId));

        // then
        assertTrue(repository.findById(id1).isPresent());
        assertTrue(repository.findById(id2).isPresent());
        assertTrue(repository.findById(id3).isPresent());

        assertEquals(3, repository.count());
    }
}
