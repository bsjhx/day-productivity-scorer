package com.bsjhx.dayproductivityscore.infrastructure.query;

import com.bsjhx.dayproductivityscore.application.query.DayQuery.DayScoreView;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayLocked;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayRated;
import org.springframework.context.event.EventListener;

import java.util.Optional;

public class DayProjectionUpdater {

    private final InMemoryReadDb db;

    public DayProjectionUpdater(InMemoryReadDb db) {
        this.db = db;
    }

    @EventListener
    public void on(DayRated event) {
        Optional<DayScoreView> byDate = db.findByDate(event.dayId().id());

        if (byDate.isPresent()) {
            var current = byDate.get();
            db.save(new DayScoreView(current.date(), event.score().getScore(), current.locked()));
        } else {
            db.save(new DayScoreView(event.dayId().id(), event.score().getScore(), false));
        }
    }

    @EventListener
    public void on(DayLocked event) {
        Optional<DayScoreView> byDate = db.findByDate(event.dayId().id());

        if (byDate.isPresent()) {
            var current = byDate.get();
            db.save(new DayScoreView(current.date(), current.score(), true));
        } else {
            throw new IllegalStateException("DayLocked event received for a day that does not exist in the read model");
        }
    }
}