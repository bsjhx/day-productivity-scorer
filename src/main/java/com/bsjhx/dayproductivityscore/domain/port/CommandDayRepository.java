package com.bsjhx.dayproductivityscore.domain.port;

import com.bsjhx.dayproductivityscore.domain.DayAggregate;

import java.util.Optional;
import java.util.UUID;

public interface CommandDayRepository {
    Optional<DayAggregate> findById(UUID id);

    void save(DayAggregate day);

}
