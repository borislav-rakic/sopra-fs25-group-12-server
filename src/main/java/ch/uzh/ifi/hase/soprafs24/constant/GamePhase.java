package ch.uzh.ifi.hase.soprafs24.constant;

public enum GamePhase {
    PRESTART, // Game initialized, not yet started
    PASSING, // Players pass 3 cards (Left, Right, Across)
    FIRSTTRICK, // First trick being played (2♣ lead)
    NORMALTRICK, // Middle tricks (after first)
    FINALTRICK, // Final trick (#13)
    RESULT, // Score calculated for the game
    FINISHED, // Game done, results shown
    ABORTED; // Game was aborted (only by game owner)

    public boolean isNotActive() {
        return this == FINISHED || this == ABORTED;
    }

    public boolean inTrick() {
        return this == FIRSTTRICK || this == NORMALTRICK || this == FINALTRICK;
    }
}