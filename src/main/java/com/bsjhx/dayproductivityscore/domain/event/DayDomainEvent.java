package com.bsjhx.dayproductivityscore.domain.event;

import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.DayScore;

public sealed interface DayDomainEvent {

    record DayRated(DayId dayId, DayScore score) implements DayDomainEvent {}

    record DayLocked(DayId dayId) implements DayDomainEvent {}
}
