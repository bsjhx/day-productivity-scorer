package com.bsjhx.dayproductivityscore.domain.port;

import com.bsjhx.dayproductivityscore.domain.DayAggregate;
import com.bsjhx.dayproductivityscore.domain.DayId;

import java.util.Optional;

public interface DayRepository {
    Optional<DayAggregate> findById(DayId dayId);

    void save(DayAggregate day);

}
