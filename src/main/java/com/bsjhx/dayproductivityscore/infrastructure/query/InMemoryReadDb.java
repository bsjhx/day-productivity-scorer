package com.bsjhx.dayproductivityscore.infrastructure.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryReadDb {

    private final Map<LocalDate, DayScoreView> days = new HashMap<>();

    Optional<DayScoreView> findByDate(LocalDate date) {
        return Optional.ofNullable(days.get(date));
    }

    List<DayScoreView> findInRange(LocalDate from, LocalDate to) {
        return days.entrySet().stream()
                .filter(a -> a.getKey().isAfter(from.minus(Period.ofDays(1))))
                .filter(a -> null == to || a.getKey().isBefore(to))
                .map(Map.Entry::getValue)
                .toList();
    }

    void save(DayScoreView dayScoreView) {
        days.put(dayScoreView.date(), dayScoreView);
    }
}
