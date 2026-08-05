package com.bsjhx.dayproductivityscore.application.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QueryDayRepository {

    List<DayScoreView> findInRange(UUID userId, LocalDate from, LocalDate to);

    Optional<DayScoreView> findDayByDateAndUser(UUID userId, LocalDate date);
}
