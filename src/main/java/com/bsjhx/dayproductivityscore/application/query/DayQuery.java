package com.bsjhx.dayproductivityscore.application.query;

import java.time.LocalDate;

public sealed interface DayQuery {

    record GetDaysInRangeQuery(LocalDate from, LocalDate to) implements DayQuery {}

    record DayScoreView(LocalDate date, int score, boolean locked) {}
}
