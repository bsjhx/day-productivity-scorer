package com.bsjhx.dayproductivityscore.infrastructure;

import java.time.LocalDate;

public class DayEntity {

    private LocalDate dayId;
    private int dayScore;
    private boolean locked;

    public DayEntity(LocalDate dayId, int dayScore, boolean locked) {
        this.dayId = dayId;
        this.dayScore = dayScore;
        this.locked = locked;
    }

    public LocalDate getDayId() {
        return dayId;
    }

    public void setDayId(LocalDate dayId) {
        this.dayId = dayId;
    }

    public int getDayScore() {
        return dayScore;
    }

    public void setDayScore(int dayScore) {
        this.dayScore = dayScore;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
