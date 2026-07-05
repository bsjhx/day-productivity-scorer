package com.bsjhx.dayproductivityscore.domain;

import java.time.LocalDate;

public record DayId (String id) {

    public static DayId of(LocalDate date) {
        return new DayId(date.toString());
    }

    public static DayId of(String id) {
        return new DayId(id);
    }

}
