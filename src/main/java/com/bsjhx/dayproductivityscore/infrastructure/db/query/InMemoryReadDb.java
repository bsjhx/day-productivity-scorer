package com.bsjhx.dayproductivityscore.infrastructure.db.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryReadDb {

    private final Map<LocalDate, DayScoreView> days = new HashMap<>();

    Optional<DayScoreView> findByDate(LocalDate date) {
        return Optional.ofNullable(days.get(date));
    }

    void save(DayScoreView dayScoreView) {
        days.put(dayScoreView.date(), dayScoreView);
    }
}
