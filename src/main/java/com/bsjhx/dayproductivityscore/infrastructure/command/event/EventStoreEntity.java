package com.bsjhx.dayproductivityscore.infrastructure.command.event;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("event_store")
public class EventStoreEntity {

    @Id
    private Long id;
    private String aggregateId;
    private int version;
    private String eventType;
    private String payload;
    private Instant createdAt;

    public EventStoreEntity() {
    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}