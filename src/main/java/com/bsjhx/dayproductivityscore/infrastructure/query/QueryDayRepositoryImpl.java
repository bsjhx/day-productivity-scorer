package com.bsjhx.dayproductivityscore.infrastructure.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery;
import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;
import com.bsjhx.dayproductivityscore.application.query.QueryDayRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                        p.getUserId(),
                        p.getDate(),
                        p.getScore(),
                        p.isLocked()
                ))
                .toList();
    }

    @Override
    public Optional<DayScoreView> findDayByDateAndUser(UUID userId, LocalDate date) {
        return jdbcRepository.findByUserIdAndDate(userId, date)
                .map(p -> new DayQuery.DayScoreView(
                        p.getId(),
                        p.getUserId(),
                        p.getDate(),
                        p.getScore(),
                        p.isLocked()
                ));
    }

    private List<DayProjection> getDayProjections(LocalDate from, LocalDate to) {
        if (to == null) {
            return jdbcRepository.findFromDate(from.toString());
        } else {
            return jdbcRepository.findByDateRange(from.toString(), to.toString());
        }
    }

}
