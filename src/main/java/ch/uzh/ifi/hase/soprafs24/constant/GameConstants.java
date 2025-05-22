package ch.uzh.ifi.hase.soprafs24.constant;

/**
 * Constants that are used throughout the Project.
 */
public final class GameConstants {

    public static final int FULL_DECK_CARD_COUNT = 52;

    public static final String CARD_CODE_REGEX = "^[02-9JQKA][CDHS]$";

    public static final String CARD_CODE_HAND_REGEX = "^[02-9JQKA][CDHS](,[02-9JQKA][CDHS])*$";

    public static final String SEED_PREFIX = "seed--";

    public static final int POLLING_INTERVAL_MS = 1000;

    // This is not final, because it may change
    public static boolean PREVENT_OVERPOLLING = false;

    public static final int MAX_TRICK_SIZE = 4;

    public static final int MAX_TRICK_NUMBER = 13;

    public static final String TWO_OF_CLUBS = "2C";

    public static final String QUEEN_OF_SPADES = "QS";

    public static final int HOST_TIME_OUT_SECONDS = 30;

    public static final int NON_HOST_TIME_OUT_SECONDS = 25;

    public static final int TRICK_DELAY_MS = 1500;

    // This is not final, because it may change.
    public static boolean HOSTS_ARE_ALLOWED_TO_LEAVE_THE_MATCH = true;

    public static boolean DO_POPULATE_TEST_USERS = false;

    private GameConstants() {
        // Prevent instantiation
    }
}