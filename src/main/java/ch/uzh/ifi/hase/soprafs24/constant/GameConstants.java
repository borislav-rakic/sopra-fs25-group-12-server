package ch.uzh.ifi.hase.soprafs24.constant;

/**
 * Constants that are used throughout the Project.
 */
public final class GameConstants {

    public static final int FULL_DECK_CARD_COUNT = 52;

    public static final String CARD_CODE_REGEX = "^[02-9JQKA][CDHS]$";

    public static final String SEED_PREFIX = "seed--";

    public static final boolean PLAY_ALL_AI_TURNS_AT_ONCE = false;

    public static final int POLLING_INTERVAL = 1000;

    public static final boolean PREVENT_OVERPOLLING = false;

    public static final int MAX_TRICK_SIZE = 4;

    public static final int MAX_TRICK_NUMBER = 13;

    public static final String TWO_OF_CLUBS = "2C";

    public static final String QUEEN_OF_SPADES = "QS";

    private GameConstants() {
        // Prevent instantiation
    }
}