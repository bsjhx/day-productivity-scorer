package com.bsjhx.dayproductivityscore.infrastructure.db.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery;
import com.bsjhx.dayproductivityscore.application.query.QueryDayRepository;

import java.time.LocalDate;
import java.util.Optional;

import static com.bsjhx.dayproductivityscore.application.query.DayQuery.*;

public class InMemoryQueryDayRepository implements QueryDayRepository {

    private final InMemoryReadDb inMemoryDb;

    public InMemoryQueryDayRepository(InMemoryReadDb inMemoryDb) {
        this.inMemoryDb = inMemoryDb;
    }

    @Override
    public Optional<DayScoreView> findByDate(LocalDate date) {
        return inMemoryDb.findByDate(date);
    }

}
