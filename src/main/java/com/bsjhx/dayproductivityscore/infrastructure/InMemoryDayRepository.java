package com.bsjhx.dayproductivityscore.infrastructure;

import com.bsjhx.dayproductivityscore.domain.DayAggregate;
import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.port.DayReadOnlyRepository;
import com.bsjhx.dayproductivityscore.domain.port.DayRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDayRepository implements DayRepository, DayReadOnlyRepository {

    private final DayToEntityMapper dayToEntityMapper = new DayToEntityMapper();

    private final Map<LocalDate, DayEntity> days = new ConcurrentHashMap<>();

    @Override
    public Optional<DayAggregate> findById(DayId dayId) {
        return Optional.ofNullable(days.get(dayId.id()))
                .map(dayToEntityMapper::toDomain);
    }

    @Override
    public void save(DayAggregate day) {
        days.put(day.getId().id(), dayToEntityMapper.toEntity(day));
    }

    @Override
    public Optional<DayAggregate> findByDate(LocalDate date) {
        return Optional.ofNullable(days.get(date))
                .map(dayToEntityMapper::toDomain);
    }

    @Override
    public List<DayAggregate> findByMonthAndYear(int year, int month) {
        return List.of();
    }
}
