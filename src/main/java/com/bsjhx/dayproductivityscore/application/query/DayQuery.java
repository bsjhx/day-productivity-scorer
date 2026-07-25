package com.bsjhx.dayproductivityscore.application.query;

import java.time.LocalDate;
import java.util.UUID;

public sealed interface DayQuery {

    record GetDaysInRangeQuery(LocalDate from, LocalDate to) implements DayQuery {}

    // todo rename
    record DayScoreView(UUID id, UUID userId, LocalDate date, int score, boolean locked) {}
}
