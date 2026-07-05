package com.bsjhx.dayproductivityscore.application.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.GetDayScoreDetailsQuery;
import com.bsjhx.dayproductivityscore.domain.port.DayReadOnlyRepository;

import java.util.Optional;

public class DayQueryService {

    private final DayReadOnlyRepository repository;

    public DayQueryService(DayReadOnlyRepository repository) {
        this.repository = repository;
    }

    public Optional<DayScoreView> handle(GetDayScoreDetailsQuery query) {
        return repository.findByDate(query.date())
                .map(d -> new DayScoreView(query.date(), d.getDayScore().getScore(), d.isLocked()));
    }

}
