package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.constant.Strategy;

import java.util.Map;

public class StrategyRegistry {

    private static final Map<Integer, Strategy> STRATEGY_MAP = Map.of(
            1, Strategy.LEFTMOST,
            2, Strategy.RANDOM,
            3, Strategy.DUMPHIGHESTFACEFIRST,
            4, Strategy.GETRIDOFCLUBSTHENHEARTS,
            5, Strategy.PREFERBLACK,
            6, Strategy.VOIDSUIT,
            7, Strategy.HYPATIA,
            8, Strategy.GARY,
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