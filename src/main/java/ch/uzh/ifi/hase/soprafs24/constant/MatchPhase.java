package ch.uzh.ifi.hase.soprafs24.constant;

public enum MatchPhase {
    SETUP, // Players are still joining
    READY, // All 4 players confirmed, waiting to begin
    IN_PROGRESS, // At least one game started
    BETWEEN_GAMES, // One game finished, another about to start
    FINISHED, // All games are done, match is over and if it is still accessed, HTML summary
              // is shown.
    ABORTED; // Aborted by user

    public boolean inGame() {
        return this == IN_PROGRESS || this == BETWEEN_GAMES;
    }

    public boolean over() {
        return this == FINISHED || this == ABORTED;
    }
}