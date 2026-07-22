package com.bsjhx.dayproductivityscore.infrastructure.command;

import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventJsonMapper {

    private final ObjectMapper objectMapper;

    public EventJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(DayDomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends DayDomainEvent> T deserialize(
            String json,
            Class<T> type
    ) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
