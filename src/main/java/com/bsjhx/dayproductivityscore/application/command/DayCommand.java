package com.bsjhx.dayproductivityscore.application.command;

import com.bsjhx.dayproductivityscore.domain.DayScore;
import java.time.LocalDate;

public sealed interface DayCommand {
    record RateDay(LocalDate date, DayScore score) implements DayCommand {}
    record LockDay(LocalDate date) implements DayCommand {}
}
