package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.constant.Strategy;

import java.util.Map;

public class StrategyRegistry {

    private static final Map<Integer, Strategy> STRATEGY_MAP = Map.of(
            1, Strategy.RANDOM,
            2, Strategy.WAVERING,
            3, Strategy.PREFERRED,
            4, Strategy.LEFTMOST,
            5, Strategy.RIGHTMOST,
            6, Strategy.GETRIDOFCLUBSTHENHEARTS,
            7, Strategy.GARY,
            8, Strategy.ALBERT,
            9, Strategy.ADA);

    private StrategyRegistry() {
        // utility class: prevent instantiation
    }

    public static Strategy getStrategyForUserId(Long userId) {
        if (userId == null) {
            return Strategy.RANDOM; // or throw if preferred
        }
        return STRATEGY_MAP.getOrDefault(userId.intValue(), Strategy.RANDOM);
    }
}