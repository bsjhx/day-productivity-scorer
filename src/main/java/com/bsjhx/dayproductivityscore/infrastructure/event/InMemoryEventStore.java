package com.bsjhx.dayproductivityscore.infrastructure.event;

import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryEventStore {
    private final Map<String, List<DayDomainEvent>> streams = new ConcurrentHashMap<>();

    public List<DayDomainEvent> loadStream(String aggregateId) {
        return streams.getOrDefault(aggregateId, new ArrayList<>());
    }

    public void append(String aggregateId, List<DayDomainEvent> newEvents) {
        List<DayDomainEvent> currentStream = streams.computeIfAbsent(aggregateId, k -> new ArrayList<>());
        currentStream.addAll(newEvents);
    }
}
