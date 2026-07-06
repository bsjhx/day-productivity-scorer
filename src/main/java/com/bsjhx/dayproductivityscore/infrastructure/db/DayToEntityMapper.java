package com.bsjhx.dayproductivityscore.infrastructure.db;

import com.bsjhx.dayproductivityscore.domain.DayAggregate;
import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.DayScore;

public class DayToEntityMapper {

    public DayAggregate toDomain(DayEntity entity) {
        return DayAggregate.reconstitute(
                DayId.of(entity.getDayId()),
                DayScore.withScore(entity.getDayScore()),
                entity.isLocked()
        );
    }

    public DayEntity toEntity(DayAggregate day) {
        return new DayEntity(
                day.getId().id(),
                day.getDayScore().getScore(),
                day.isLocked()
        );
    }
}
