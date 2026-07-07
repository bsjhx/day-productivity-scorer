package com.bsjhx.dayproductivityscore.application.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.GetDaysInRangeQuery;

import java.util.List;

public class DayQueryService {

    private final QueryDayRepository repository;

    public DayQueryService(QueryDayRepository repository) {
        this.repository = repository;
    }

    public List<DayScoreView> handle(GetDaysInRangeQuery query) {
        return repository.findInRange(query.from(), query.to());
    }

}
