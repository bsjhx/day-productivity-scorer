package com.bsjhx.dayproductivityscore.infrastructure.query;

import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayLocked;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayRated;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

public class DayProjectionListener {

    private final DayProjectionJdbcRepository dayProjectionRepository;

    public DayProjectionListener(DayProjectionJdbcRepository dayProjectionRepository) {
        this.dayProjectionRepository = dayProjectionRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(DayRated event) {
        Optional<DayProjection> day = dayProjectionRepository.findById(event.id());

        DayProjection projection;
        if (day.isPresent()) {
            projection = day.get();
            projection.setScore(event.score().getScore());
        } else {
            projection = new DayProjection(event.id(), event.userId(), event.dayId().id(), event.score().getScore(), false);
        }

        dayProjectionRepository.save(projection);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(DayLocked event) {
        Optional<DayProjection> byDate = dayProjectionRepository.findById(event.id());

        if (byDate.isPresent()) {
            var current = byDate.get();
            current.setLocked(true);
            dayProjectionRepository.save(current);
        } else {
            throw new IllegalStateException("DayLocked event received for a day that does not exist in the read model");
        }
    }
}