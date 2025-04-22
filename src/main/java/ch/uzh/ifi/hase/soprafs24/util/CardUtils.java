package ch.uzh.ifi.hase.soprafs24.util;

import java.util.List;
import java.util.stream.Collectors;

public class CardUtils {

    /**
     * Calculates a sortable numeric value for a card code.
     * Sorting order is: Clubs < Diamonds < Spades < Hearts, then by rank.
     *
     * Examples:
     * "2C" -> 12, "QS" -> 62, "AH" -> 84
     *
     * @param card Card code, e.g. "2C", "10H", "QS"
     * @return numeric value for sorting
     */
    public static int calculateCardOrder(String card) {
        if (card == null || card.length() < 2 || card.length() > 3) {
            throw new IllegalArgumentException("Invalid card format: " + card);
        }

        String rankStr = card.substring(0, card.length() - 1); // e.g. "10" or "Q"
        char suitChar = card.charAt(card.length() - 1);        // e.g. 'H'

        int rankValue;
        switch (rankStr) {
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
            case "10":
            case "0": // in case 10 is represented as "0"
                rankValue = 10;
                break;
            default:
                if (rankStr.matches("[2-9]")) {
                    rankValue = Integer.parseInt(rankStr);
                } else {
                    throw new IllegalArgumentException("Invalid rank: " + rankStr);
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

    /**
     * Compare two cards by their sort order.
     * @param cardA first card code (e.g. "2C")
     * @param cardB second card code (e.g. "QS")
     * @return -1 if A < B, 0 if equal, 1 if A > B
     */
    public static int compareCards(String cardA, String cardB) {
        return Integer.compare(calculateCardOrder(cardA), calculateCardOrder(cardB));
    }

    /**
     * Sorts a list of card codes (e.g. ["AH", "2C", "QS"]) based on Hearts rules order.
     * @param cardCodes list of card codes
     * @return new sorted list
     */
    public static List<String> sortCardsByOrder(List<String> cardCodes) {
        return cardCodes.stream()
            .sorted(CardUtils::compareCards)
            .collect(Collectors.toList());
    }
}
