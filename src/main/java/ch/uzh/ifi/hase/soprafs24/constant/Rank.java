package ch.uzh.ifi.hase.soprafs24.constant;

public enum Rank {
    A("A"), _2("2"), _3("3"), _4("4"), _5("5"),
    _6("6"), _7("7"), _8("8"), _9("9"), _0("0"),
    J("J"), Q("Q"), K("K");

    private final String label;

    Rank(String label) {
        this.label = label;
    }

    public String getSymbol() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
