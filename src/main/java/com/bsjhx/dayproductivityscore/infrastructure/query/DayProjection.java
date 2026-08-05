package com.bsjhx.dayproductivityscore.infrastructure.query;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Table("day_projection")
public class DayProjection implements Persistable<UUID> {

    @Id
    private UUID id;

    private UUID userId;

    private LocalDate date;

    private int score;

    private boolean isLocked;

    @Transient
    private boolean isNew;

    public DayProjection() {
        this.isNew = false;
    }

    public DayProjection(UUID id, UUID userId, LocalDate date, int score, boolean isLocked) {
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.score = score;
        this.isLocked = isLocked;
        this.isNew = true;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

}
