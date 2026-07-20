package com.bsjhx.dayproductivityscore.domain;

import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayLocked;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayRated;
import lombok.Getter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DayAggregate {

    private final DayId dayId;

    @Getter
    private int expectedVersion = 0;

    @Getter
    private DayScore dayScore = DayScore.NONE;

    @Getter
    private boolean locked = false;

    private final List<DayDomainEvent> changes = new ArrayList<>();

    public DayAggregate(DayId dayId) {
        this.dayId = dayId;
    }

    public static DayAggregate recreate(DayId dayId, List<DayDomainEvent> changes) {
        DayAggregate dayAggregate = new DayAggregate(dayId);
        dayAggregate.expectedVersion = changes.size();
        changes.forEach(dayAggregate::apply);
        return dayAggregate;
    }

    public void rate(DayScore dayScore) {
        if (dayScore == null) {
            throw new IllegalArgumentException("DayScore cannot be null");
        }
        if (locked) {
            throw new IllegalStateException("DayScore cannot be changed when the day is locked");
        }
        if (dayId.id().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Must not rate a day in the future");
        }

        raise(new DayRated(dayId, dayScore));
    }

    public void lock() {
        if (locked) {
            return;
        }
        raise(new DayLocked(dayId));
    }

    private void raise(DayDomainEvent  event) {
        apply(event);
        changes.add(event);
    }

    public DayId getId() {
        return dayId;
    }

    public List<DayDomainEvent> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    public void clearChanges() {
        changes.clear();
    }

    private void apply(DayDomainEvent event) {
        switch (event) {
            case DayRated rated -> this.dayScore = rated.score();
            case DayLocked ignored -> this.locked = true;
        }
    }

}
