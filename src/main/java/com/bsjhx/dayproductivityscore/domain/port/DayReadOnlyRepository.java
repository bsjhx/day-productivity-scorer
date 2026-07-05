package com.bsjhx.dayproductivityscore.domain.port;

import com.bsjhx.dayproductivityscore.domain.DayAggregate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DayReadOnlyRepository {
    Optional<DayAggregate> findByDate(LocalDate date);
    List<DayAggregate> findByMonthAndYear(int year, int month);
}