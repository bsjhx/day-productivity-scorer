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

public class InMemoryDayReadOnlyRepository implements DayReadOnlyRepository {

    private final InMemoryDb inMemoryDb;

    private final DayToEntityMapper dayToEntityMapper = new DayToEntityMapper();

    private final Map<LocalDate, DayEntity> days = new ConcurrentHashMap<>();

    public InMemoryDayReadOnlyRepository(InMemoryDb inMemoryDb) {
        this.inMemoryDb = inMemoryDb;
    }

    @Override
    public Optional<DayAggregate> findByDate(LocalDate date) {
        return inMemoryDb.findByDate(date)
                .map(dayToEntityMapper::toDomain);
    }

    @Override
    public List<DayAggregate> findByMonthAndYear(int year, int month) {
        return List.of();
    }
}
