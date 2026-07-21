package com.bsjhx.dayproductivityscore.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
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

    private static final Map<Integer, DayScore> SCORE_MAP =
        Stream.of(values()).collect(Collectors.toMap(DayScore::getScore, ds -> ds));

    public static DayScore withScore(int score) {
        DayScore dayScore = SCORE_MAP.get(score);
        if (dayScore == null) {
            throw new IllegalArgumentException("Invalid score: " + score);
        }
        return dayScore;
    }
}
