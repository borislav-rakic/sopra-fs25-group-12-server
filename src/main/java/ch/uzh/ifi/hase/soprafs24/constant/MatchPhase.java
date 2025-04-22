package ch.uzh.ifi.hase.soprafs24.constant;

public enum MatchPhase {
    SETUP,         // Players are still joining
    READY,         // All 4 players confirmed, waiting to begin
    IN_PROGRESS,   // At least one game started
    BETWEEN_GAMES, // One game finished, another about to start
    FINISHED,      // All games done, match is over
    ABORTED;       // Aborted by user
}