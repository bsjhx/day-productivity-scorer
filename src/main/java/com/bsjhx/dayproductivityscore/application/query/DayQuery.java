package com.bsjhx.dayproductivityscore.application.query;

import com.bsjhx.dayproductivityscore.domain.DayScore;

import java.time.LocalDate;

public sealed interface DayQuery {

    record GetDaysInRangeQuery(LocalDate from, LocalDate to) implements DayQuery {}

    record DayScoreView(LocalDate date, int score, boolean locked) {

        public static DayScoreView unrated(LocalDate date) {
            return new DayScoreView(date, DayScore.NONE.getScore(), false);
        }

    }
}
