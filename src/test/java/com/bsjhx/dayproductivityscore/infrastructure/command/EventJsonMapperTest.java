package com.bsjhx.dayproductivityscore.infrastructure.command;

import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.DayScore;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayLocked;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayRated;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventJsonMapperTest {

    private EventJsonMapper mapper;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mapper = new EventJsonMapper(objectMapper);
    }

    @Test
    void shouldSerializeDayRatedEvent() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayRated event = new DayRated(id, dayId, DayScore.FIVE, userId);

        // when
        String json = mapper.serialize(event);

        // then
        assertNotNull(json);
        assertFalse(json.isEmpty());
    }

    @Test
    void shouldSerializeDayLockedEvent() {
        // given
        UUID id = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 21));
        DayLocked event = new DayLocked(id, dayId);

        // when
        String json = mapper.serialize(event);

        // then
        assertNotNull(json);
        assertFalse(json.isEmpty());
    }

    @Test
    void shouldDeserializeDayRatedEvent() {
        // given
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID userId = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
        String json = String.format("{\"id\":\"%s\",\"dayId\":{\"id\":\"2026-07-20\"},\"score\":\"FIVE\",\"userId\":\"%s\"}",
            id, userId);

        // when
        DayRated event = mapper.deserialize(json, DayRated.class);

        // then
        assertNotNull(event);
        assertEquals(id, event.id());
        assertEquals(LocalDate.of(2026, 7, 20), event.dayId().id());
        assertEquals(DayScore.FIVE, event.score());
        assertEquals(userId, event.userId());
    }

    @Test
    void shouldDeserializeDayLockedEvent() {
        // given
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String json = String.format("{\"id\":\"%s\",\"dayId\":{\"id\":\"2026-07-21\"}}", id);

        // when
        DayLocked event = mapper.deserialize(json, DayLocked.class);

        // then
        assertNotNull(event);
        assertEquals(id, event.id());
        assertEquals(LocalDate.of(2026, 7, 21), event.dayId().id());
    }

    @Test
    void shouldSerializeAndDeserializeDayRatedEvent() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 22));
        DayRated originalEvent = new DayRated(id, dayId, DayScore.THREE, userId);

        // when
        String json = mapper.serialize(originalEvent);
        DayRated deserializedEvent = mapper.deserialize(json, DayRated.class);

        // then
        assertEquals(originalEvent.id(), deserializedEvent.id());
        assertEquals(originalEvent.dayId(), deserializedEvent.dayId());
        assertEquals(originalEvent.score(), deserializedEvent.score());
        assertEquals(originalEvent.userId(), deserializedEvent.userId());
    }

    @Test
    void shouldSerializeAndDeserializeDayLockedEvent() {
        // given
        UUID id = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 23));
        DayLocked originalEvent = new DayLocked(id, dayId);

        // when
        String json = mapper.serialize(originalEvent);
        DayLocked deserializedEvent = mapper.deserialize(json, DayLocked.class);

        // then
        assertEquals(originalEvent.id(), deserializedEvent.id());
        assertEquals(originalEvent.dayId(), deserializedEvent.dayId());
    }

    @Test
    void shouldThrowExceptionWhenSerializationFails() {
        // given
        ObjectMapper brokenMapper = new ObjectMapper();
        // Note: Not registering JavaTimeModule will cause serialization to fail for LocalDate
        EventJsonMapper brokenEventMapper = new EventJsonMapper(brokenMapper);
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayRated event = new DayRated(id, dayId, DayScore.FIVE, userId);

        // when & then
        assertThrows(RuntimeException.class, () -> brokenEventMapper.serialize(event));
    }

    @Test
    void shouldThrowExceptionWhenDeserializationFails() {
        // given
        String invalidJson = "{invalid json}";

        // when & then
        assertThrows(RuntimeException.class, () ->
            mapper.deserialize(invalidJson, DayRated.class)
        );
    }

    @Test
    void shouldHandleAllDayScoreValues() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayScore[] allScores = {DayScore.NONE, DayScore.ZERO, DayScore.ONE,
                                DayScore.TWO, DayScore.THREE, DayScore.FOUR, DayScore.FIVE};

        // when & then
        for (DayScore score : allScores) {
            DayRated event = new DayRated(id, dayId, score, userId);
            String json = mapper.serialize(event);
            DayRated deserialized = mapper.deserialize(json, DayRated.class);

            assertEquals(score, deserialized.score(),
                "Failed to serialize/deserialize score: " + score);
        }
    }

    @Test
    void shouldPreserveEventTypeInformation() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayRated ratedEvent = new DayRated(id, dayId, DayScore.FOUR, userId);
        DayLocked lockedEvent = new DayLocked(id, dayId);

        // when
        String ratedJson = mapper.serialize(ratedEvent);
        String lockedJson = mapper.serialize(lockedEvent);

        // then
        DayRated deserializedRated = mapper.deserialize(ratedJson, DayRated.class);
        DayLocked deserializedLocked = mapper.deserialize(lockedJson, DayLocked.class);

        assertInstanceOf(DayRated.class, deserializedRated);
        assertInstanceOf(DayLocked.class, deserializedLocked);
        assertEquals(ratedEvent.dayId(), deserializedRated.dayId());
        assertEquals(lockedEvent.dayId(), deserializedLocked.dayId());
    }

    @Test
    void shouldHandleDifferentDateFormats() {
        // given
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        DayId dayId1 = DayId.of(LocalDate.of(2026, 1, 1));  // Beginning of year
        DayId dayId2 = DayId.of(LocalDate.of(2026, 12, 31)); // End of year
        DayRated event1 = new DayRated(id, dayId1, DayScore.ONE, userId);
        DayRated event2 = new DayRated(id, dayId2, DayScore.FIVE, userId);

        // when
        String json1 = mapper.serialize(event1);
        String json2 = mapper.serialize(event2);
        DayRated deserialized1 = mapper.deserialize(json1, DayRated.class);
        DayRated deserialized2 = mapper.deserialize(json2, DayRated.class);

        // then
        assertEquals(dayId1, deserialized1.dayId());
        assertEquals(dayId2, deserialized2.dayId());
    }
}
