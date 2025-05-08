package ch.uzh.ifi.hase.soprafs24.constant;

public enum GamePhase {
    PRESTART, // Game initialized, not yet started
    WAITING_FOR_EXTERNAL_API, // Game is waiting asynchronously for external API
    PASSING, // Players pass 3 cards (Left, Right, Across)
    SKIP_PASSING, // Every fourth round, there is no PASSING
    FIRSTTRICK, // First trick being played (2♣ lead)
    NORMALTRICK, // Middle tricks (after first)
    FINALTRICK, // Final trick (#13)
    RESULT, // Score calculated for the game
    FINISHED, // Game done, results shown
    ABORTED; // Game was aborted (only by game owner)

    public boolean isNotActive() {
        return this == FINISHED || this == ABORTED;
    }

    public boolean onGoing() {
        return this == PRESTART // Game initialized, not yet started
                || this == WAITING_FOR_EXTERNAL_API // Game is waiting asynchronously for external API
                || this == PASSING // Players pass 3 cards (Left, Right, Across)
                || this == SKIP_PASSING // Every fourth round, there is no PASSING
                || this == FIRSTTRICK // First trick being played (2♣ lead)
                || this == NORMALTRICK // Middle tricks (after first)
                || this == FINALTRICK // Final trick (#13)
                || this == RESULT; // Score calculated for the game
    }

    public boolean inTrick() {
        return this == FIRSTTRICK || this == NORMALTRICK || this == FINALTRICK;
    }

    public boolean inPassing() {
        return this == PASSING;
    }
}
