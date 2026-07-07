package com.bsjhx.dayproductivityscore.application.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;

import java.time.LocalDate;
import java.util.List;

public interface QueryDayRepository {

    List<DayScoreView> findInRange(LocalDate from, LocalDate to);
}
