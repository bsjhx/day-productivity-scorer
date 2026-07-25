package com.bsjhx.dayproductivityscore.domain.event;

import com.bsjhx.dayproductivityscore.domain.DayId;
import com.bsjhx.dayproductivityscore.domain.DayScore;

import java.util.UUID;

public sealed interface DayDomainEvent {

    record DayCreated(UUID id, DayId dayId, UUID userId) implements DayDomainEvent {}

    record DayRated(UUID id, DayId dayId, DayScore score, UUID userId) implements DayDomainEvent {}

    record DayLocked(UUID id, DayId dayId) implements DayDomainEvent {}
}
