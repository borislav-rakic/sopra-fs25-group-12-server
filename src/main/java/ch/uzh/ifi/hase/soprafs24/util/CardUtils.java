package ch.uzh.ifi.hase.soprafs24.util;

public class CardUtils {

    public static int calculateCardOrder(String card) {
        if (card == null || card.length() != 2) {
            throw new IllegalArgumentException("Invalid card format: " + card);
        }

        String rankChar = card.substring(0, 1);
        char suitChar = card.charAt(1);

        int rankValue;
        switch (rankChar) {
            case "J":
                rankValue = 11;
                break;
            case "Q":
                rankValue = 12;
                break;
            case "K":
                rankValue = 13;
                break;
            case "A":
                rankValue = 14;
                break;
            default:
                if (rankChar.matches("[2-9]")) {
                    rankValue = Integer.parseInt(rankChar);
                } else if (rankChar.equals("0")) {
                    rankValue = 10;
                } else {
                    throw new IllegalArgumentException("Invalid rank: " + rankChar);
                }
        }

        int suitValue;
        switch (suitChar) {
            case 'C':
                suitValue = 10;
                break;
            case 'D':
                suitValue = 30;
                break;
            case 'S':
                suitValue = 50;
                break;
            case 'H':
                suitValue = 70;
                break;
            default:
                throw new IllegalArgumentException("Invalid suit: " + suitChar);
        }

        return rankValue + suitValue;
    }
}
