package com.bsjhx.dayproductivityscore.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DayScore {
    NONE(-1),
    ZERO(0),
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5);

    private final int score;

    public int getScore() {
        return score;
    }

    public static DayScore withScore(int score) {
        for (DayScore dayScore : DayScore.values()) {
            if (dayScore.getScore() == score) {
                return dayScore;
            }
        }
        throw new IllegalArgumentException("Invalid score: " + score);
    }
}
