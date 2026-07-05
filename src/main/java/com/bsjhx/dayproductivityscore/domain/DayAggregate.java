package com.bsjhx.dayproductivityscore.domain;

import lombok.Getter;

public class DayAggregate {

    private final DayId dayId;

    @Getter
    private DayScore dayScore = DayScore.NONE;

    @Getter
    private boolean locked = false;

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

        // todo
        // addDomainEvent(new DayScoreChangedEvent(dayId, dayScore));
    }

    public void lock() {
        this.locked = true;
    }

    public DayId getId() {
        return dayId;
    }

}
