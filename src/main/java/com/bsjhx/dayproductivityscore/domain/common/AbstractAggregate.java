package com.bsjhx.dayproductivityscore.domain.common;

import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class AbstractAggregate {

    private UUID id;

    @Getter
    @Setter(value = AccessLevel.PROTECTED)
    private int version = 0;

    private final List<DayDomainEvent> changes = new ArrayList<>();

    public List<DayDomainEvent> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    public void clearChanges() {
        changes.clear();
    }

    protected void raise(DayDomainEvent  event) {
        apply(event);
        changes.add(event);
    }

    protected abstract void apply(DayDomainEvent event);

}
