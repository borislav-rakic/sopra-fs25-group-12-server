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
        return this.fullName; // "Hearts", "Clubs", etc.
    }

    @Override
    public String toString() {
        return fullName;
    }
}
