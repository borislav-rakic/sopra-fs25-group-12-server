package ch.uzh.ifi.hase.soprafs24.constant;

/**
 * Constants that are used throughout the Project.
 */
public final class GameConstants {

    public static final int FULL_DECK_CARD_COUNT = 52;

    public static final String CARD_CODE_REGEX = "^[02-9JQKA][CDHS]$";

    public static final String SEED_PREFIX = "seed--";

    private GameConstants() {
        // Prevent instantiation
    }
}