package com.bsjhx.dayproductivityscore.infrastructure.query;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Getter
@Setter
@Table("day_projection")
public class DayProjection implements Persistable<LocalDate> {

    @Id
    private LocalDate id;

    private int score;

    private boolean isLocked;

    @Transient
    private boolean isNew;

    public DayProjection() {
        this.isNew = false;
    }

    public DayProjection(LocalDate dayId, int score, boolean isLocked) {
        this.id = dayId;
        this.score = score;
        this.isLocked = isLocked;
        this.isNew = true;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

}
