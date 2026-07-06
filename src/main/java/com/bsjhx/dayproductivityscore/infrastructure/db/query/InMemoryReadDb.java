package com.bsjhx.dayproductivityscore.infrastructure.db.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;
import io.micrometer.observation.ObservationFilter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryReadDb {

    private final Map<LocalDate, DayScoreView> days = new HashMap<>();

    public Optional<DayScoreView> findByDate(LocalDate date) {
        return Optional.ofNullable(days.get(date));
    }
}
