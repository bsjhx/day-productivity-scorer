package com.bsjhx.dayproductivityscore.infrastructure.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;
import com.bsjhx.dayproductivityscore.application.query.QueryDayRepository;

import java.time.LocalDate;
import java.util.List;

public class QueryDayRepositoryImpl implements QueryDayRepository {

    private final DayProjectionJdbcRepository jdbcRepository;

    public QueryDayRepositoryImpl(DayProjectionJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public List<DayScoreView> findInRange(LocalDate from, LocalDate to) {
        var projections = getDayProjections(from, to);
        return projections.stream()
                .map(p -> new DayQuery.DayScoreView(
                        p.getId(),
                        p.getScore(),
                        p.isLocked()
                ))
                .toList();
    }

    private List<DayProjection> getDayProjections(LocalDate from, LocalDate to) {
        if (to == null) {
            return jdbcRepository.findFromDate(from.toString());
        } else {
            return jdbcRepository.findByDateRange(from.toString(), to.toString());
        }
    }

}
