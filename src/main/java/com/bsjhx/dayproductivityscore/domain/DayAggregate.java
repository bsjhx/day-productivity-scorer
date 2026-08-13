package com.bsjhx.dayproductivityscore.domain;

import com.bsjhx.dayproductivityscore.domain.common.AbstractAggregate;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayLocked;
import com.bsjhx.dayproductivityscore.domain.event.DayDomainEvent.DayRated;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class DayAggregate extends AbstractAggregate {

    private DayId dayId;

    @Getter
    private DayScore dayScore = DayScore.NONE;

    @Getter
    private boolean locked = false;

    @Getter
    private UUID userId;

    public DayAggregate() {
    }

    public static DayAggregate recreate(List<DayDomainEvent> changes) {
        DayAggregate dayAggregate = new DayAggregate();
        dayAggregate.setVersion(changes.size());
        changes.forEach(dayAggregate::apply);

        return dayAggregate;
    }

    public static DayAggregate create(UUID id, LocalDate date, UUID userId) {
        var day = new DayAggregate();

        day.raise(new DayDomainEvent.DayCreated(id, DayId.of(date), userId));

        return day;
    }

    public void rate(DayScore dayScore) {
        if (dayScore == null) {
            throw new DomainException("DayScore cannot be null");
        }
        if (locked) {
            throw new DomainException("DayScore cannot be changed when the day is locked");
        }
        if (dayId.id().isAfter(LocalDate.now())) {
            throw new DomainException("Must not rate a day in the future");
        }

        raise(new DayRated(id, dayId, dayScore, userId));
    }

    public void lock() {
        if (locked) {
            return;
        }
        raise(new DayLocked(id, dayId));
    }

    protected void apply(DayDomainEvent event) {
        switch (event) {
            case DayDomainEvent.DayRated rated -> this.dayScore = rated.score();
            case DayDomainEvent.DayLocked ignored -> this.locked = true;
            case DayDomainEvent.DayCreated dayCreated -> {
                this.id = dayCreated.id();
                this.dayId = dayCreated.dayId();
                this.userId = dayCreated.userId();
            }
        }
    }

}
