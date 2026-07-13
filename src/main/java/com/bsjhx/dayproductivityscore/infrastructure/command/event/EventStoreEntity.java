package com.bsjhx.dayproductivityscore.infrastructure.command.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Setter
@Getter
@Table("event_store")
public class EventStoreEntity {

    @Id
    private Long id;
    private String aggregateId;
    private int version;
    private String eventType;
    private String payload;
    private Instant createdAt;

    public EventStoreEntity(Long id, String aggregateId, int version, String eventType, String payload, Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.version = version;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public static EventStoreEntity of(String aggregateId, int version, String eventType, String payload) {
        return new EventStoreEntity(null, aggregateId, version, eventType, payload, Instant.now());
    }

}