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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class EventStoreRepositoryIntegrationTest {

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
        UUID id = UUID.randomUUID();
        Optional<DayAggregate> result = repository.findById(id);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSaveNewAggregateAndPersistEvents() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);
        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.clearChanges();
        aggregate.rate(DayScore.FIVE);

        repository.save(aggregate);

        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(id.toString());
        assertEquals(1, events.size());
        EventStoreEntity event = events.getFirst();
        assertEquals(id.toString(), event.getAggregateId());
        assertEquals(1, event.getVersion());
        assertEquals("DayRated", event.getEventType());
        assertNotNull(event.getPayload());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void shouldLoadAggregateFromEventStore() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);
        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.rate(DayScore.FOUR);
        repository.save(aggregate);

        Optional<DayAggregate> loaded = repository.findById(id);

        assertTrue(loaded.isPresent());
        DayAggregate loadedAggregate = loaded.get();
        assertEquals(id, loadedAggregate.getId());
        assertEquals(DayScore.FOUR, loadedAggregate.getDayScore());
        assertFalse(loadedAggregate.isLocked());
        assertEquals(2, loadedAggregate.getVersion());
        assertTrue(loadedAggregate.getChanges().isEmpty());
    }

    @Test
    void shouldSaveMultipleEvents() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);
        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.rate(DayScore.TWO);
        aggregate.rate(DayScore.FOUR);
        aggregate.lock();

        repository.save(aggregate);

        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(id.toString());
        assertEquals(4, events.size());
        assertEquals(1, events.get(0).getVersion());
        assertEquals(2, events.get(1).getVersion());
        assertEquals(3, events.get(2).getVersion());
        assertEquals(4, events.get(3).getVersion());
    }

    @Test
    void shouldLoadAggregateWithMultipleEvents() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);
        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.rate(DayScore.TWO);
        aggregate.rate(DayScore.FIVE);
        aggregate.lock();
        repository.save(aggregate);

        Optional<DayAggregate> loaded = repository.findById(id);

        assertTrue(loaded.isPresent());
        DayAggregate loadedAggregate = loaded.get();
        assertEquals(DayScore.FIVE, loadedAggregate.getDayScore());
        assertTrue(loadedAggregate.isLocked());
        assertEquals(4, loadedAggregate.getVersion());
    }

    @Test
    void shouldClearChangesAfterSave() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);
        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.rate(DayScore.THREE);

        repository.save(aggregate);

        assertTrue(aggregate.getChanges().isEmpty());
    }

    @Test
    void shouldNotSaveWhenNoChanges() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);
        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.clearChanges();

        repository.save(aggregate);

        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(id.toString());
        assertTrue(events.isEmpty());
    }

    @Test
    void shouldAppendEventsToExistingAggregate() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);

        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.rate(DayScore.TWO);
        repository.save(aggregate);

        DayAggregate loaded = repository.findById(id).orElseThrow();
        loaded.rate(DayScore.FIVE);
        repository.save(loaded);

        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(id.toString());
        assertEquals(3, events.size());
        assertEquals(1, events.get(0).getVersion());
        assertEquals(2, events.get(1).getVersion());
        assertEquals(3, events.get(2).getVersion());

        DayAggregate reloaded = repository.findById(id).orElseThrow();
        assertEquals(DayScore.FIVE, reloaded.getDayScore());
        assertEquals(3, reloaded.getVersion());
    }

    @Test
    void shouldThrowExceptionOnConcurrentModification() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);

        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.rate(DayScore.TWO);
        repository.save(aggregate);

        DayAggregate aggregate1 = repository.findById(id).orElseThrow();
        DayAggregate aggregate2 = repository.findById(id).orElseThrow();

        aggregate1.rate(DayScore.FOUR);
        repository.save(aggregate1);

        aggregate2.rate(DayScore.FIVE);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            repository.save(aggregate2)
        );
        assertTrue(exception.getMessage().contains("Aggregate modified by another process"));
        assertTrue(exception.getMessage().contains("Expected version: 2, actual: 3"));
    }

    @Test
    void shouldSerializeAndDeserializeDayRatedEvent() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);
        DayId dayId = DayId.of(date);
        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.rate(DayScore.FOUR);
        repository.save(aggregate);

        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(id.toString());

        EventStoreEntity eventEntity = events.stream()
                .filter(e -> "DayRated".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();
        DayRated event = objectMapper.readValue(eventEntity.getPayload(), DayRated.class);
        assertEquals(dayId, event.dayId());
        assertEquals(DayScore.FOUR, event.score());
    }

    @Test
    void shouldSerializeAndDeserializeDayLockedEvent() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);
        DayId dayId = DayId.of(date);
        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.rate(DayScore.THREE);
        aggregate.lock();
        repository.save(aggregate);

        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(id.toString());

        EventStoreEntity lockEvent = events.stream()
                .filter(e -> "DayLocked".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();
        assertEquals("DayLocked", lockEvent.getEventType());
        DayLocked event = objectMapper.readValue(lockEvent.getPayload(), DayLocked.class);
        assertEquals(dayId, event.dayId());
    }

    @Test
    void shouldHandleMultipleAggregates() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date1 = LocalDate.of(2026, 7, 20);
        LocalDate date2 = LocalDate.of(2026, 7, 21);
        LocalDate date3 = LocalDate.of(2026, 7, 22);

        DayAggregate aggregate1 = DayAggregate.create(id1, date1, userId);
        aggregate1.rate(DayScore.TWO);

        DayAggregate aggregate2 = DayAggregate.create(id2, date2, userId);
        aggregate2.rate(DayScore.FOUR);
        aggregate2.lock();

        DayAggregate aggregate3 = DayAggregate.create(id3, date3, userId);
        aggregate3.rate(DayScore.FIVE);

        repository.save(aggregate1);
        repository.save(aggregate2);
        repository.save(aggregate3);

        Optional<DayAggregate> loaded1 = repository.findById(id1);
        Optional<DayAggregate> loaded2 = repository.findById(id2);
        Optional<DayAggregate> loaded3 = repository.findById(id3);

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
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);
        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.rate(DayScore.ONE);
        aggregate.rate(DayScore.TWO);
        aggregate.rate(DayScore.THREE);
        aggregate.rate(DayScore.FOUR);
        aggregate.lock();
        repository.save(aggregate);

        List<EventStoreEntity> events = jdbcRepository.findByAggregateId(id.toString());

        assertEquals(6, events.size());
        for (int i = 0; i < events.size(); i++) {
            assertEquals(i + 1, events.get(i).getVersion());
        }

        DayAggregate loaded = repository.findById(id).orElseThrow();
        assertEquals(DayScore.FOUR, loaded.getDayScore());
        assertTrue(loaded.isLocked());
    }

    @Test
    void shouldQueryMaxVersionCorrectly() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);
        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.rate(DayScore.TWO);
        aggregate.rate(DayScore.THREE);
        aggregate.rate(DayScore.FOUR);
        repository.save(aggregate);

        Integer maxVersion = jdbcRepository.findMaxVersionByAggregateId(id.toString());

        assertNotNull(maxVersion);
        assertEquals(4, maxVersion);
    }

    @Test
    void shouldReturnNullMaxVersionForNonExistentAggregate() {
        UUID id = UUID.randomUUID();
        Integer maxVersion = jdbcRepository.findMaxVersionByAggregateId(id.toString());
        assertNull(maxVersion);
    }

    @Test
    void shouldHandleCompleteEventSourcingWorkflow() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 20);
        DayAggregate aggregate = DayAggregate.create(id, date, userId);
        aggregate.rate(DayScore.TWO);
        repository.save(aggregate);

        DayAggregate day2 = repository.findById(id).orElseThrow();
        day2.rate(DayScore.THREE);
        repository.save(day2);

        DayAggregate day3 = repository.findById(id).orElseThrow();
        day3.rate(DayScore.FOUR);
        repository.save(day3);

        DayAggregate day4 = repository.findById(id).orElseThrow();
        day4.lock();
        repository.save(day4);

        DayAggregate finalAggregate = repository.findById(id).orElseThrow();

        assertEquals(DayScore.FOUR, finalAggregate.getDayScore());
        assertTrue(finalAggregate.isLocked());
        assertEquals(5, finalAggregate.getVersion());

        List<EventStoreEntity> allEvents = jdbcRepository.findByAggregateId(id.toString());
        assertEquals(5, allEvents.size());
    }
}
