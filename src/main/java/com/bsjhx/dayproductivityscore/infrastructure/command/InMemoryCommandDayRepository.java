package com.bsjhx.dayproductivityscore.infrastructure.command;

import com.bsjhx.dayproductivityscore.domain.DayAggregate;
import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;
import com.bsjhx.dayproductivityscore.domain.port.CommandDayRepository;
import com.bsjhx.dayproductivityscore.infrastructure.event.InMemoryEventStore;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

public class InMemoryCommandDayRepository implements CommandDayRepository {

    private final InMemoryEventStore eventStore;

    private final ApplicationEventPublisher eventPublisher;

    public InMemoryCommandDayRepository(InMemoryEventStore eventStore, ApplicationEventPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<DayAggregate> findById(DayId dayId) {
        List<DayDomainEvent> history = eventStore.loadStream(dayId.id().toString());

        if (history.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(DayAggregate.recreate(dayId, history));
    }

    @Override
    public void save(DayAggregate day) {
        var newEvents = day.getChanges();
        eventStore.append(day.getId().toString(), newEvents);
        newEvents.forEach(eventPublisher::publishEvent);
        day.clearChanges();
    }

}
