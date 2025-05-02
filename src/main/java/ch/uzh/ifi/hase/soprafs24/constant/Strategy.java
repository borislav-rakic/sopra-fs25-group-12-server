package ch.uzh.ifi.hase.soprafs24.constant;

public enum Strategy {
    // Always take the leftmost card.
    LEFTMOST(1),

    // Take any playable card.
    RANDOM(2),

    // Try to get rid of QS,AH,KH,QH,JH,AS,AD,AC,KS, ... in that order.
    DUMPHIGHESTFACEFIRST(3),

    // Try to get rid of Clubs as quickly as possible, then hearts.
    GETRIDOFCLUBSTHENHEARTS(4),

    // Try to keep black cards and discard red cards
    PREFERBLACK(5),

    // Get rid of the suit with the fewest items first.
    VOIDSUIT(6),

    // Think like Hypatia
    HYPATIA(7),

    // Think like Gary
    GARY(8),

    // Think like Ada
    ADA(9);

    private final int code;

    Strategy(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
