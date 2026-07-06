package com.bsjhx.dayproductivityscore.infrastructure.db.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery;
import com.bsjhx.dayproductivityscore.domain.DayAggregate;
import com.bsjhx.dayproductivityscore.domain.port.QueryDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.db.DayEntity;
import com.bsjhx.dayproductivityscore.infrastructure.db.DayToEntityMapper;
import com.bsjhx.dayproductivityscore.infrastructure.db.command.InMemoryWriteMemoryDb;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryQueryDayRepository implements QueryDayRepository {

    private final InMemoryReadDb inMemoryDb;

    private final DayToEntityMapper dayToEntityMapper = new DayToEntityMapper();

    private final Map<LocalDate, DayEntity> days = new ConcurrentHashMap<>();

    public InMemoryQueryDayRepository(InMemoryReadDb inMemoryDb) {
        this.inMemoryDb = inMemoryDb;
    }

    @Override
    public Optional<DayQuery.DayScoreView> findByDate(LocalDate date) {
        return inMemoryDb.findByDate(date);
    }

}
