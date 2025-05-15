package ch.uzh.ifi.hase.soprafs24.model;

import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

public class Card {
    private String code;
    private String image;
    private String rank;
    private String suit;
    private int value;
    private int cardOrder;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        if (code == null || code.length() < 2 || code.length() > 3) {
            throw new IllegalArgumentException("Invalid card code: " + code);
        }
        this.code = code.toUpperCase();
        this.rank = extractRank(code);
        this.suit = extractSuit(code);
        this.value = calculateValue(this.rank);
        this.image = generateImageUrl(code);
        this.cardOrder = CardUtils.calculateCardOrder(code);
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
        return String.valueOf(code.charAt(code.length() - 1)).toUpperCase(); // "H", "D", "C", "S"
    }

    private String generateImageUrl(String code) {
        return "https://deckofcardsapi.com/static/img/" + code + ".png";
    }

    private int calculateValue(String rank) {
        return switch (rank) {
            case "2" -> 2;
            case "3" -> 3;
            case "4" -> 4;
            case "5" -> 5;
            case "6" -> 6;
            case "7" -> 7;
            case "8" -> 8;
            case "9" -> 9;
            case "0" -> 10;
            case "J" -> 11;
            case "Q" -> 12;
            case "K" -> 13;
            case "A" -> 14;
            default -> 0;
        };
    }

    public int getValue() {
        return value;
    }

    public String getSuitName() {
        return switch (suit) {
            case "H" -> "Hearts";
            case "D" -> "Diamonds";
            case "C" -> "Clubs";
            case "S" -> "Spades";
            default -> "Unknown";
        };
    }
}
