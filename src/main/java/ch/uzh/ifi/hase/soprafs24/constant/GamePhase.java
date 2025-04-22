package ch.uzh.ifi.hase.soprafs24.constant;

public enum GamePhase {
    PRESTART,     // Game initialized, not yet started
    PASSING,      // Players pass 3 cards (Left, Right, Across)
    FIRSTROUND,   // First trick being played (2♣ lead)
    NORMALROUND,  // Middle tricks (after first)
    FINALROUND,   // Final trick (#13)
    RESULT,       // Score calculated for the game
    FINISHED,     // Game done, results shown
    ABORTED;      // Game was aborted (only by game owner)
}