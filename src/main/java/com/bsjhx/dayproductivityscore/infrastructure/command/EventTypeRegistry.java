package com.bsjhx.dayproductivityscore.infrastructure.command;

import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EventTypeRegistry {

    @SuppressWarnings("unchecked")
    private static final Map<String, Class<? extends DayDomainEvent>> EVENT_TYPES_MAP =
            Arrays.stream(DayDomainEvent.class.getPermittedSubclasses())
                    .map(c -> (Class<? extends DayDomainEvent>) c)
                    .collect(Collectors.toMap(
                            Class::getSimpleName,
                            Function.identity()
                    ));

    public static Class<? extends DayDomainEvent> resolve(String type) {
        return Optional.ofNullable(EVENT_TYPES_MAP.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Unknown event: " + type));
    }
}