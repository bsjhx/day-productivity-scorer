package com.bsjhx.dayproductivityscore.application.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.GetDayScoreDetailsQuery;

import java.util.Optional;

public class DayQueryService {

    private final QueryDayRepository repository;

    public DayQueryService(QueryDayRepository repository) {
        this.repository = repository;
    }

    public Optional<DayScoreView> handle(GetDayScoreDetailsQuery query) {
        return repository.findByDate(query.date());
    }

}
