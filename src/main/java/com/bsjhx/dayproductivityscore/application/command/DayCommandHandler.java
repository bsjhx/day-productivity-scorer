package com.bsjhx.dayproductivityscore.application.command;

import com.bsjhx.dayproductivityscore.application.command.DayCommand.LockDay;
import com.bsjhx.dayproductivityscore.application.command.DayCommand.RateDay;
import com.bsjhx.dayproductivityscore.application.query.DayQuery;
import com.bsjhx.dayproductivityscore.application.query.QueryDayRepository;
import com.bsjhx.dayproductivityscore.domain.DayAggregate;
import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.port.CommandDayRepository;

import java.util.Optional;
import java.util.UUID;

public class DayCommandHandler {

    private final CommandDayRepository dayRepository;
    private final QueryDayRepository queryDayRepository;

    public DayCommandHandler(CommandDayRepository dayRepository, QueryDayRepository queryDayRepository) {
        this.dayRepository = dayRepository;
        this.queryDayRepository = queryDayRepository;
    }

    public void handle(RateDay command) {
        Optional<DayQuery.DayScoreView> dayQuery = queryDayRepository.findDayByDateAndUser(command.userId(), command.date());

        if (dayQuery.isPresent()) {
            var id = dayQuery.get().id();
            var day = dayRepository.findById(id).orElseThrow();
            day.rate(command.score());
            dayRepository.save(day);
        } else {
            var newDay = DayAggregate.create(UUID.randomUUID(), command.date(), command.userId());
            newDay.rate(command.score());
            dayRepository.save(newDay);
        }
    }

    public void handle(LockDay command) {
        Optional<DayQuery.DayScoreView> dayQuery = queryDayRepository.findDayByDateAndUser(command.userId(), command.date());

        if (dayQuery.isEmpty()) {
            throw new IllegalArgumentException("Day not found for date: " + command.date());
        }

        var id = dayQuery.get().id();
        var day = dayRepository.findById(id).orElseThrow();
        day.lock();
        dayRepository.save(day);
    }
}
