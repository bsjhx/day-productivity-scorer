package com.bsjhx.dayproductivityscore.application.query;

import java.time.LocalDate;

public sealed interface DayQuery {

    record GetDayScoreDetailsQuery(LocalDate date) implements DayQuery {}

    record DayScoreView(LocalDate date, int score, boolean locked) {}
}
