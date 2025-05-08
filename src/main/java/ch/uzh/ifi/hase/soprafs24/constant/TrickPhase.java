package ch.uzh.ifi.hase.soprafs24.constant;

public enum TrickPhase {
    READYFORFIRSTCARD, // Trick is empty, waiting for first card (2♣ holder starts)
    RUNNINGTRICK, // Trick in progress (1–3 cards played)
    TRICKJUSTCOMPLETED, // 4th card played, display result delay (UI animations etc.)
    PROCESSINGTRICK; // Delay expired, ready to assign points and clear trick

    public boolean inTransition() {
        return this == TRICKJUSTCOMPLETED || this == PROCESSINGTRICK;
    }

    public boolean inPlay() {
        return this == READYFORFIRSTCARD || this == RUNNINGTRICK;
    }
}