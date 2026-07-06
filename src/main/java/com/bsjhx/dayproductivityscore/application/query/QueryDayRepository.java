package com.bsjhx.dayproductivityscore.application.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;

import java.time.LocalDate;
import java.util.Optional;

public interface QueryDayRepository {
    Optional<DayScoreView> findByDate(LocalDate date);
}
