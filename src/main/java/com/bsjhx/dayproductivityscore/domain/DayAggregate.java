package com.bsjhx.dayproductivityscore.domain;

import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayLocked;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayRated;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DayAggregate {

    private final DayId dayId;

    @Getter
    private DayScore dayScore = DayScore.NONE;

    @Getter
    private boolean locked = false;

    private final List<DayDomainEvent> changes = new ArrayList<>();

    public DayAggregate(DayId dayId) {
        this.dayId = dayId;
    }

    private DayAggregate(DayId dayId, DayScore dayScore, boolean locked) {
        this.dayId = dayId;
        this.dayScore = dayScore;
        this.locked = locked;
    }

    public static DayAggregate reconstitute(DayId dayId, DayScore dayScore, boolean locked) {
        return new DayAggregate(dayId, dayScore, locked);
    }

    public void rate(DayScore dayScore) {
        if (dayScore == null) {
            throw new IllegalArgumentException("DayScore cannot be null");
        }
        if (locked) {
            throw new IllegalStateException("DayScore cannot be changed when the day is locked");
        }

        this.dayScore = dayScore;

        changes.add(new DayRated(dayId, dayScore));
    }

    public void lock() {
        this.locked = true;

        changes.add(new DayLocked(dayId));
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

}
