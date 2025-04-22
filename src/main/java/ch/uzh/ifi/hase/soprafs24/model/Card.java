package ch.uzh.ifi.hase.soprafs24.model;

import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

public class Card {
    private String code;
    private String image;
    private String rank;
    private String suit;
    private int value;
    private int cardOrder; // <- New field

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
        this.rank = extractRank(code);
        this.suit = extractSuit(code);
        this.image = generateImageUrl(code);
        this.value = calculateValue(this.rank);
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

    public int getValue() {
        return value;
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

    private int calculateValue(String rank) {
        return switch (rank) {
            case "A" -> 14;
            case "K" -> 13;
            case "Q" -> 12;
            case "J" -> 11;
            case "10" -> 10;
            case "9" -> 9;
            case "8" -> 8;
            case "7" -> 7;
            case "6" -> 6;
            case "5" -> 5;
            case "4" -> 4;
            case "3" -> 3;
            case "2" -> 2;
            default -> 0;
        };
    }
}
