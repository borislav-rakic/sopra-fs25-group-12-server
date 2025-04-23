package ch.uzh.ifi.hase.soprafs24.model;

import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

public class Card {
    private String code;
    private String image;
    private String rank;
    private String suit;
    private String value;
    private int cardOrder; // <- New field

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        this.rank = extractRank(code);
        this.suit = extractSuit(code);
        this.image = generateImageUrl(code);
        this.cardOrder = CardUtils.calculateCardOrder(code); // <- Set cardOrder here
    }

    public String getImage() {
        return image;
    }

    public String getRank() {
        return rank;
    }

    public String getSuit() {
        return suit;
    }

    public int getCardOrder() {
        return cardOrder;
    }

    private String extractRank(String code) {
        if (code.length() == 2) {
            String firstChar = code.substring(0, 1);
            return firstChar.equals("0") ? "10" : firstChar;
        } else if (code.length() == 3) {
            return "10";
        }
        return "Unknown";
    }

    private String extractSuit(String code) {
        char suitChar = code.charAt(code.length() - 1);
        return switch (suitChar) {
            case 'H' -> "Hearts";
            case 'D' -> "Diamonds";
            case 'C' -> "Clubs";
            case 'S' -> "Spades";
            default -> "Unknown";
        };
    }

    private String generateImageUrl(String code) {
        return "https://deckofcardsapi.com/static/img/" + code + ".png";
    }
}
