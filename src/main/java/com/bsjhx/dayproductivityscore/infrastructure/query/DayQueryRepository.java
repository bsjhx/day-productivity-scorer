package com.bsjhx.dayproductivityscore.infrastructure.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;
import com.bsjhx.dayproductivityscore.application.query.QueryDayRepository;

import java.time.LocalDate;
import java.util.List;

public class DayQueryRepository implements QueryDayRepository {

    private final SpringDataJdbcDayProjectionRepository repository;

    public DayQueryRepository(SpringDataJdbcDayProjectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DayScoreView> findInRange(LocalDate from, LocalDate to) {
        List<DayProjection> projections;
        if (to == null) {
            projections = repository.findFromDate(from.toString());
        } else {
            projections = repository.findByDateRange(from.toString(), to.toString());
        }
        return projections.stream()
                .map(p -> new DayQuery.DayScoreView(
                        p.getId(),
                        p.getScore(),
                        p.isLocked()
                ))
                .toList();
    }

    ;
}
