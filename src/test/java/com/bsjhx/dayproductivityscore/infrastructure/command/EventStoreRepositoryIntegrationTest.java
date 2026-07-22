package com.bsjhx.dayproductivityscore.infrastructure.command;

import com.bsjhx.dayproductivityscore.domain.DayAggregate;
import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.DayScore;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayLocked;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayRated;
import com.bsjhx.dayproductivityscore.infrastructure.command.event.EventStoreEntity;
import com.bsjhx.dayproductivityscore.infrastructure.command.event.EventStoreJdbcRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class EventStoreRepositoryIntegrationTest {

    // subject
    private EventStoreRepository repository;

    @Autowired
    private EventStoreJdbcRepository jdbcRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;


    @BeforeEach
    void setUp() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        repository = new EventStoreRepository(jdbcRepository, objectMapper, eventPublisher);
        jdbcRepository.deleteAll();
    }

    @Test
    void shouldReturnEmptyWhenAggregateNotFound() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));

        // when
        Optional<DayAggregate> result = repository.findById(dayId);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSaveNewAggregateAndPersistEvents() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.FIVE);

        // when
        repository.save(aggregate);

        // then
        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(dayId.id().toString());
        assertEquals(1, events.size());

        EventStoreEntity event = events.getFirst();
        assertEquals(dayId.id().toString(), event.getAggregateId());
        assertEquals(1, event.getVersion());
        assertEquals("DayRated", event.getEventType());
        assertNotNull(event.getPayload());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void shouldLoadAggregateFromEventStore() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.FOUR);
        repository.save(aggregate);

        // when
        Optional<DayAggregate> loaded = repository.findById(dayId);

        // then
        assertTrue(loaded.isPresent());
        DayAggregate loadedAggregate = loaded.get();
        assertEquals(dayId, loadedAggregate.getId());
        assertEquals(DayScore.FOUR, loadedAggregate.getDayScore());
        assertFalse(loadedAggregate.isLocked());
        assertEquals(1, loadedAggregate.getExpectedVersion());
        assertTrue(loadedAggregate.getChanges().isEmpty());
    }

    @Test
    void shouldSaveMultipleEvents() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.TWO);
        aggregate.rate(DayScore.FOUR);
        aggregate.lock();

        // when
        repository.save(aggregate);

        // then
        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(dayId.id().toString());
        assertEquals(3, events.size());
        assertEquals(1, events.get(0).getVersion());
        assertEquals(2, events.get(1).getVersion());
        assertEquals(3, events.get(2).getVersion());
    }

    @Test
    void shouldLoadAggregateWithMultipleEvents() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.TWO);
        aggregate.rate(DayScore.FIVE);
        aggregate.lock();
        repository.save(aggregate);

        // when
        Optional<DayAggregate> loaded = repository.findById(dayId);

        // then
        assertTrue(loaded.isPresent());
        DayAggregate loadedAggregate = loaded.get();
        assertEquals(DayScore.FIVE, loadedAggregate.getDayScore());
        assertTrue(loadedAggregate.isLocked());
        assertEquals(3, loadedAggregate.getExpectedVersion());
    }

    @Test
    void shouldClearChangesAfterSave() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.THREE);

        // when
        repository.save(aggregate);

        // then
        assertTrue(aggregate.getChanges().isEmpty());
    }

    @Test
    void shouldNotSaveWhenNoChanges() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);

        // when
        repository.save(aggregate);

        // then
        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(dayId.id().toString());
        assertTrue(events.isEmpty());
    }

    @Test
    void shouldAppendEventsToExistingAggregate() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));

        // First save
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.TWO);
        repository.save(aggregate);

        // Load and modify
        DayAggregate loaded = repository.findById(dayId).orElseThrow();
        loaded.rate(DayScore.FIVE);

        // when - second save
        repository.save(loaded);

        // then
        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(dayId.id().toString());
        assertEquals(2, events.size());
        assertEquals(1, events.get(0).getVersion());
        assertEquals(2, events.get(1).getVersion());

        // Verify loaded aggregate has correct state
        DayAggregate reloaded = repository.findById(dayId).orElseThrow();
        assertEquals(DayScore.FIVE, reloaded.getDayScore());
        assertEquals(2, reloaded.getExpectedVersion());
    }

    @Test
    void shouldThrowExceptionOnConcurrentModification() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));

        // Initial save
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.TWO);
        repository.save(aggregate);

        // Load same aggregate in two different contexts
        DayAggregate aggregate1 = repository.findById(dayId).orElseThrow();
        DayAggregate aggregate2 = repository.findById(dayId).orElseThrow();

        // Modify and save first aggregate
        aggregate1.rate(DayScore.FOUR);
        repository.save(aggregate1);

        // Try to save second aggregate (should fail - stale version)
        aggregate2.rate(DayScore.FIVE);

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            repository.save(aggregate2)
        );
        assertTrue(exception.getMessage().contains("Aggregate modified by another process"));
        assertTrue(exception.getMessage().contains("Expected version: 1, actual: 2"));
    }

    @Test
    void shouldSerializeAndDeserializeDayRatedEvent() throws Exception {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.FOUR);
        repository.save(aggregate);

        // when
        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(dayId.id().toString());

        // then
        EventStoreEntity eventEntity = events.getFirst();
        DayRated event = objectMapper.readValue(eventEntity.getPayload(), DayRated.class);
        assertEquals(dayId, event.dayId());
        assertEquals(DayScore.FOUR, event.score());
    }

    @Test
    void shouldSerializeAndDeserializeDayLockedEvent() throws Exception {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.THREE);
        aggregate.lock();
        repository.save(aggregate);

        // when
        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(dayId.id().toString());

        // then
        EventStoreEntity lockEvent = events.get(1);
        assertEquals("DayLocked", lockEvent.getEventType());
        DayLocked event = objectMapper.readValue(lockEvent.getPayload(), DayLocked.class);
        assertEquals(dayId, event.dayId());
    }

    @Test
    void shouldHandleMultipleAggregates() {
        // given
        DayId day1 = DayId.of(LocalDate.of(2026, 7, 20));
        DayId day2 = DayId.of(LocalDate.of(2026, 7, 21));
        DayId day3 = DayId.of(LocalDate.of(2026, 7, 22));

        DayAggregate aggregate1 = new DayAggregate(day1);
        aggregate1.rate(DayScore.TWO);

        DayAggregate aggregate2 = new DayAggregate(day2);
        aggregate2.rate(DayScore.FOUR);
        aggregate2.lock();

        DayAggregate aggregate3 = new DayAggregate(day3);
        aggregate3.rate(DayScore.FIVE);

        // when
        repository.save(aggregate1);
        repository.save(aggregate2);
        repository.save(aggregate3);

        // then
        Optional<DayAggregate> loaded1 = repository.findById(day1);
        Optional<DayAggregate> loaded2 = repository.findById(day2);
        Optional<DayAggregate> loaded3 = repository.findById(day3);

        assertTrue(loaded1.isPresent());
        assertEquals(DayScore.TWO, loaded1.get().getDayScore());
        assertFalse(loaded1.get().isLocked());

        assertTrue(loaded2.isPresent());
        assertEquals(DayScore.FOUR, loaded2.get().getDayScore());
        assertTrue(loaded2.get().isLocked());

        assertTrue(loaded3.isPresent());
        assertEquals(DayScore.FIVE, loaded3.get().getDayScore());
        assertFalse(loaded3.get().isLocked());
    }

    @Test
    void shouldPreserveEventOrderWhenLoading() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.ONE);
        aggregate.rate(DayScore.TWO);
        aggregate.rate(DayScore.THREE);
        aggregate.rate(DayScore.FOUR);
        aggregate.lock();
        repository.save(aggregate);

        // when
        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(dayId.id().toString());

        // then
        assertEquals(5, events.size());
        for (int i = 0; i < events.size(); i++) {
            assertEquals(i + 1, events.get(i).getVersion());
        }

        // Verify order matters for final state
        DayAggregate loaded = repository.findById(dayId).orElseThrow();
        assertEquals(DayScore.FOUR, loaded.getDayScore());
        assertTrue(loaded.isLocked());
    }

    @Test
    void shouldQueryMaxVersionCorrectly() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.TWO);
        aggregate.rate(DayScore.THREE);
        aggregate.rate(DayScore.FOUR);
        repository.save(aggregate);

        // when
        Integer maxVersion = jdbcRepository.findMaxVersionByAggregateId(dayId.id().toString());

        // then
        assertNotNull(maxVersion);
        assertEquals(3, maxVersion);
    }

    @Test
    void shouldReturnNullMaxVersionForNonExistentAggregate() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));

        // when
        Integer maxVersion = jdbcRepository.findMaxVersionByAggregateId(dayId.id().toString());

        // then
        assertNull(maxVersion);
    }

    @Test
    void shouldHandleCompleteEventSourcingWorkflow() {
        // given - Day 1: Create and rate
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayAggregate aggregate = new DayAggregate(dayId);
        aggregate.rate(DayScore.TWO);
        repository.save(aggregate);

        // Day 2: Load, change rating
        DayAggregate day2 = repository.findById(dayId).orElseThrow();
        day2.rate(DayScore.THREE);
        repository.save(day2);

        // Day 3: Load, change rating again
        DayAggregate day3 = repository.findById(dayId).orElseThrow();
        day3.rate(DayScore.FOUR);
        repository.save(day3);

        // Day 4: Load and lock
        DayAggregate day4 = repository.findById(dayId).orElseThrow();
        day4.lock();
        repository.save(day4);

        // when - Final load
        DayAggregate finalAggregate = repository.findById(dayId).orElseThrow();

        // then
        assertEquals(DayScore.FOUR, finalAggregate.getDayScore());
        assertTrue(finalAggregate.isLocked());
        assertEquals(4, finalAggregate.getExpectedVersion());

        // Verify all events are stored
        List<EventStoreEntity> allEvents = jdbcRepository.findByAggregateId(dayId.id().toString());
        assertEquals(4, allEvents.size());
    }
}
