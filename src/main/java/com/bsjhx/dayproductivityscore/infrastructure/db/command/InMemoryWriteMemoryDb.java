package com.bsjhx.dayproductivityscore.infrastructure.db.command;

import com.bsjhx.dayproductivityscore.infrastructure.db.DayEntity;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryWriteMemoryDb {

    private final Map<LocalDate, DayEntity> days = new ConcurrentHashMap<>();

    Optional<DayEntity> findByDate(LocalDate date) {
        return Optional.ofNullable(days.get(date));
    }

    void save(DayEntity dayEntity) {
        days.put(dayEntity.getDayId(), dayEntity);
    }
}
