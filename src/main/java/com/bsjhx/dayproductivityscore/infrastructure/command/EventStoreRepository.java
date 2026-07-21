package com.bsjhx.dayproductivityscore.infrastructure.command;

import com.bsjhx.dayproductivityscore.domain.DayAggregate;
import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;
import com.bsjhx.dayproductivityscore.domain.port.CommandDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.command.event.EventStoreEntity;
import com.bsjhx.dayproductivityscore.infrastructure.command.event.EventStoreJdbcRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventStoreRepository implements CommandDayRepository {

    private final EventStoreJdbcRepository jdbcRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public EventStoreRepository(EventStoreJdbcRepository jdbcRepository, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher) {
        this.jdbcRepository = jdbcRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<DayAggregate> findById(DayId dayId) {
        List<EventStoreEntity> byAggregateId = jdbcRepository.findByAggregateId(dayId.id().toString());

        if (byAggregateId.isEmpty()) {
            return Optional.empty();
        }

        List<DayDomainEvent> history = byAggregateId.stream()
                .map(this::deserializeEvent)
                .toList();

        return Optional.of(DayAggregate.recreate(dayId, history));
    }

    @Override
    @Transactional
    public void save(DayAggregate day) {
        List<DayDomainEvent> newEvents = day.getChanges();
        if (newEvents.isEmpty()) return;

        Integer lastVersion = jdbcRepository.findMaxVersionByAggregateId(day.getId().id().toString());
        int currentVersion = (lastVersion != null) ? lastVersion : 0;

        if (day.getExpectedVersion() != currentVersion) {
            throw new RuntimeException(
                    "Aggregate modified by another process. Expected version: " +
                            day.getExpectedVersion() + ", actual: " + currentVersion
            );
        }

        List<EventStoreEntity> entitiesToSave = new ArrayList<>();

        for (DayDomainEvent event : newEvents) {
            currentVersion++;
            String payload = serializeEvent(event);

            entitiesToSave.add(EventStoreEntity.of(
                    day.getId().id().toString(),
                    currentVersion,
                    event.getClass().getName(),
                    payload
            ));
        }

        jdbcRepository.saveAll(entitiesToSave);

        newEvents.forEach(eventPublisher::publishEvent);

        day.clearChanges();
    }

    private String serializeEvent(DayDomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event: " + event.getClass().getSimpleName(), e);
        }
    }

    private DayDomainEvent deserializeEvent(EventStoreEntity entity) {
        try {
            Class<?> eventClass = Class.forName(entity.getEventType());
            return (DayDomainEvent) objectMapper.readValue(entity.getPayload(), eventClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event of type: " + entity.getEventType(), e);
        }
    }
}
