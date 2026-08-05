package com.bsjhx.dayproductivityscore.application.command;

import com.bsjhx.dayproductivityscore.domain.DayScore;
import java.time.LocalDate;
import java.util.UUID;

public sealed interface DayCommand {
    record RateDay(UUID userId, LocalDate date, DayScore score) implements DayCommand {}
    record LockDay(UUID userId, LocalDate date) implements DayCommand {}
}
