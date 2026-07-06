package com.bsjhx.dayproductivityscore.application.command;

import com.bsjhx.dayproductivityscore.application.command.DayCommand.LockDay;
import com.bsjhx.dayproductivityscore.application.command.DayCommand.RateDay;
import com.bsjhx.dayproductivityscore.domain.DayAggregate;
import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.port.CommandDayRepository;

public class DayCommandHandler {

    private final CommandDayRepository dayRepository;

    public DayCommandHandler(CommandDayRepository dayRepository) {
        this.dayRepository = dayRepository;
    }

    public void handle(RateDay command) {
        var dayId = DayId.of(command.date());
        var day = dayRepository.findById(dayId).orElseGet(() -> new DayAggregate(dayId));

        day.rate(command.score());

        dayRepository.save(day);
    }

    public void handle(LockDay command) {
        var dayId = DayId.of(command.date());
        var day = dayRepository.findById(dayId).orElseThrow(
            () -> new IllegalArgumentException("Day not found for date: " + command.date())
        );

        day.lock();

        dayRepository.save(day);
    }
}
