package com.bsjhx.dayproductivityscore.api.rest;

import java.time.LocalDate;

public sealed interface DayRestApiDto {
    record DayRateRequest(LocalDate day, int score) implements DayRestApiDto {}
    record SingleDayResponse(LocalDate day, int score, boolean isLocked) implements DayRestApiDto {}
}

