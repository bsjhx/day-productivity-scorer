package com.bsjhx.dayproductivityscore.infrastructure;

import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.DayScore;

public class DayEntity {

    private String dayId;
    private int dayScore;
    private boolean locked;

    public DayEntity(String dayId, int dayScore, boolean locked) {
        this.dayId = dayId;
        this.dayScore = dayScore;
        this.locked = locked;
    }

    public String getDayId() {
        return dayId;
    }

    public void setDayId(String dayId) {
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
