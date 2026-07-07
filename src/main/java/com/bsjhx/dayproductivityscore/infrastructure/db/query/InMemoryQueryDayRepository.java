package com.bsjhx.dayproductivityscore.infrastructure.db.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery;
import com.bsjhx.dayproductivityscore.application.query.QueryDayRepository;

import java.time.LocalDate;
import java.util.List;

import static com.bsjhx.dayproductivityscore.application.query.DayQuery.*;

public class InMemoryQueryDayRepository implements QueryDayRepository {

    private final InMemoryReadDb inMemoryDb;

    public InMemoryQueryDayRepository(InMemoryReadDb inMemoryDb) {
        this.inMemoryDb = inMemoryDb;
    }

    @Override
    public List<DayScoreView> findInRange(LocalDate from, LocalDate to) {
        return inMemoryDb.findInRange(from, to);
    }

}
