package com.bsjhx.dayproductivityscore.infrastructure.db.command;

import com.bsjhx.dayproductivityscore.domain.DayAggregate;
import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.port.CommandDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.db.DayEntity;
import com.bsjhx.dayproductivityscore.infrastructure.db.DayToEntityMapper;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCommandDayRepository implements CommandDayRepository {

    private final InMemoryWriteMemoryDb inMemoryDb;

    private final DayToEntityMapper dayToEntityMapper = new DayToEntityMapper();

    private final Map<LocalDate, DayEntity> days = new ConcurrentHashMap<>();

    public InMemoryCommandDayRepository(InMemoryWriteMemoryDb inMemoryDb) {
        this.inMemoryDb = inMemoryDb;
    }

    @Override
    public Optional<DayAggregate> findById(DayId dayId) {
        return inMemoryDb.findByDate(dayId.id())
                .map(dayToEntityMapper::toDomain);
    }

    @Override
    public void save(DayAggregate day) {
        inMemoryDb.save(dayToEntityMapper.toEntity(day));
    }

}
