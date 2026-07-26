package com.bsjhx.dayproductivityscore.domain;

import java.time.LocalDate;

// todo rename
public record DayId (LocalDate id) {

    public static DayId of(LocalDate date) {
        return new DayId(date);
    }

}
