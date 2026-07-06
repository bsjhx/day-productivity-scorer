package com.bsjhx.dayproductivityscore.infrastructure.db.command;

import com.bsjhx.dayproductivityscore.domain.DayAggregate;
import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.port.CommandDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.db.DayToEntityMapper;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

public class InMemoryCommandDayRepository implements CommandDayRepository {

    private final InMemoryWriteMemoryDb inMemoryDb;

    private final ApplicationEventPublisher eventPublisher;

    private final DayToEntityMapper dayToEntityMapper = new DayToEntityMapper();

    public InMemoryCommandDayRepository(InMemoryWriteMemoryDb inMemoryDb, ApplicationEventPublisher eventPublisher) {
        this.inMemoryDb = inMemoryDb;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<DayAggregate> findById(DayId dayId) {
        return inMemoryDb.findByDate(dayId.id())
                .map(dayToEntityMapper::toDomain);
    }

    @Override
    public void save(DayAggregate day) {
        inMemoryDb.save(dayToEntityMapper.toEntity(day));
        day.getChanges().forEach(eventPublisher::publishEvent);
        day.clearChanges();
    }

}
