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
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayRated event = new DayRated(dayId, DayScore.FIVE);

        // when
        String json = mapper.serialize(event);

        // then
        assertNotNull(json);
        assertFalse(json.isEmpty());
    }

    @Test
    void shouldSerializeDayLockedEvent() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 21));
        DayLocked event = new DayLocked(dayId);

        // when
        String json = mapper.serialize(event);

        // then
        assertNotNull(json);
        assertFalse(json.isEmpty());
    }

    @Test
    void shouldDeserializeDayRatedEvent() {
        // given
        String json = "{\"dayId\":{\"id\":\"2026-07-20\"},\"score\":\"FIVE\"}";

        // when
        DayRated event = mapper.deserialize(json, DayRated.class);

        // then
        assertNotNull(event);
        assertEquals(LocalDate.of(2026, 7, 20), event.dayId().id());
        assertEquals(DayScore.FIVE, event.score());
    }

    @Test
    void shouldDeserializeDayLockedEvent() {
        // given
        String json = "{\"dayId\":{\"id\":\"2026-07-21\"}}";

        // when
        DayLocked event = mapper.deserialize(json, DayLocked.class);

        // then
        assertNotNull(event);
        assertEquals(LocalDate.of(2026, 7, 21), event.dayId().id());
    }

    @Test
    void shouldSerializeAndDeserializeDayRatedEvent() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 22));
        DayRated originalEvent = new DayRated(dayId, DayScore.THREE);

        // when
        String json = mapper.serialize(originalEvent);
        DayRated deserializedEvent = mapper.deserialize(json, DayRated.class);

        // then
        assertEquals(originalEvent.dayId(), deserializedEvent.dayId());
        assertEquals(originalEvent.score(), deserializedEvent.score());
    }

    @Test
    void shouldSerializeAndDeserializeDayLockedEvent() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 23));
        DayLocked originalEvent = new DayLocked(dayId);

        // when
        String json = mapper.serialize(originalEvent);
        DayLocked deserializedEvent = mapper.deserialize(json, DayLocked.class);

        // then
        assertEquals(originalEvent.dayId(), deserializedEvent.dayId());
    }

    @Test
    void shouldThrowExceptionWhenSerializationFails() {
        // given
        ObjectMapper brokenMapper = new ObjectMapper();
        // Note: Not registering JavaTimeModule will cause serialization to fail for LocalDate
        EventJsonMapper brokenEventMapper = new EventJsonMapper(brokenMapper);
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayRated event = new DayRated(dayId, DayScore.FIVE);

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
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayScore[] allScores = {DayScore.NONE, DayScore.ZERO, DayScore.ONE,
                                DayScore.TWO, DayScore.THREE, DayScore.FOUR, DayScore.FIVE};

        // when & then
        for (DayScore score : allScores) {
            DayRated event = new DayRated(dayId, score);
            String json = mapper.serialize(event);
            DayRated deserialized = mapper.deserialize(json, DayRated.class);

            assertEquals(score, deserialized.score(),
                "Failed to serialize/deserialize score: " + score);
        }
    }

    @Test
    void shouldPreserveEventTypeInformation() {
        // given
        DayId dayId = DayId.of(LocalDate.of(2026, 7, 20));
        DayRated ratedEvent = new DayRated(dayId, DayScore.FOUR);
        DayLocked lockedEvent = new DayLocked(dayId);

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
        DayId dayId1 = DayId.of(LocalDate.of(2026, 1, 1));  // Beginning of year
        DayId dayId2 = DayId.of(LocalDate.of(2026, 12, 31)); // End of year
        DayRated event1 = new DayRated(dayId1, DayScore.ONE);
        DayRated event2 = new DayRated(dayId2, DayScore.FIVE);

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
