package ch.uzh.ifi.hase.soprafs24.constant;

public enum Suit {
    C, // Clubs
    D, // Diamonds
    S, // Spades
    H; // Hearts

    public String getSymbol() {
        return this.name(); // Returns "C", "D", "S", or "H"
    }

    public static Suit fromSymbol(String symbol) {
        for (Suit suit : values()) {
            if (suit.name().equalsIgnoreCase(symbol)) {
                return suit;
            }
        }
        throw new IllegalArgumentException("Invalid suit symbol: " + symbol);
    }

    @Override
    public String toString() {
        return this.name(); // Just returns "C", "D", etc.
    }
}
