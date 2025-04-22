package ch.uzh.ifi.hase.soprafs24.constant;

public enum Suit {
    C("Clubs"),
    D("Diamonds"),
    S("Spades"),
    H("Hearts");

    private final String fullName;

    Suit(String fullName) {
        this.fullName = fullName;
    }

    public String getSymbol() {
        return this.name(); // "C", "D", etc.
    }

    public String getFullName() {
        return this.fullName;
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
        return fullName;
    }
}
