package com.bsjhx.dayproductivityscore.infrastructure.query;

import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayLocked;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayRated;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

public class DayProjectionUpdater {

    private final SpringDataJdbcDayProjectionRepository dayProjectionRepository;

    public DayProjectionUpdater(SpringDataJdbcDayProjectionRepository dayProjectionRepository) {
        this.dayProjectionRepository = dayProjectionRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(DayRated event) {
        Optional<DayProjection> byDate = dayProjectionRepository.findById(event.dayId().id());

        DayProjection projection;
        if (byDate.isPresent()) {
            projection = byDate.get();
            projection.setScore(event.score().getScore());
        } else {
            projection = new DayProjection(event.dayId().id(), event.score().getScore(), false);
        }

        dayProjectionRepository.save(projection);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(DayLocked event) {
        Optional<DayProjection> byDate = dayProjectionRepository.findById(event.dayId().id());

        if (byDate.isPresent()) {
            var current = byDate.get();
            current.setLocked(true);
            dayProjectionRepository.save(current).markAsNotNew();
        } else {
            throw new IllegalStateException("DayLocked event received for a day that does not exist in the read model");
        }
    }
}