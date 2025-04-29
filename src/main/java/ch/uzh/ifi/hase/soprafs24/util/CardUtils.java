package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.model.Card;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardUtils {
    private static final Logger log = LoggerFactory.getLogger(CardUtils.class);

    /**
     * Creates a full Card object from its code (e.g. "QS", "0H", "10D").
     */
    public static Card fromCode(String code) {
        if (code == null || code.length() < 2 || code.length() > 3) {
            throw new IllegalArgumentException("Invalid card code: " + code);
        }

        Card card = new Card();
        card.setCode(code); // This triggers all derived field setting inside Card
        return card;
    }

    /**
     * Calculates a sortable numeric value for a card code.
     * Clubs < Diamonds < Spades < Hearts, then by rank.
     */
    public static int calculateCardOrder(String cardCode) {
        if (cardCode == null || cardCode.length() < 2 || cardCode.length() > 3) {
            throw new IllegalArgumentException("Invalid card format: " + cardCode);
        }

        String rankStr = cardCode.substring(0, cardCode.length() - 1);
        char suitChar = cardCode.charAt(cardCode.length() - 1);

        int rankValue = switch (rankStr) {
            case "J" -> 11;
            case "Q" -> 12;
            case "K" -> 13;
            case "A" -> 14;
            case "10", "0" -> 10;
            default -> {
                if (rankStr.matches("[2-9]")) {
                    yield Integer.parseInt(rankStr);
                }
                throw new IllegalArgumentException("Invalid rank: " + rankStr);
            }
        };

        int suitValue = switch (suitChar) {
            case 'C' -> 10;
            case 'D' -> 30;
            case 'S' -> 50;
            case 'H' -> 70;
            default -> throw new IllegalArgumentException("Invalid suit: " + suitChar);
        };

        return rankValue + suitValue;
    }

    /**
     * Compare two cards by order.
     */
    public static int compareCards(String cardA, String cardB) {
        return Integer.compare(calculateCardOrder(cardA), calculateCardOrder(cardB));
    }

    /**
     * Sort list of Card objects based on their calculated order.
     */
    public static List<Card> sortCardsByCardOrder(List<Card> cards) {
        return cards.stream()
                .sorted(Comparator.comparingInt(Card::getCardOrder))
                .collect(Collectors.toList());
    }

    public static Card fromGameStats(GameStats gs) {
        if (gs == null || gs.getRank() == null || gs.getSuit() == null) {
            throw new IllegalArgumentException("Invalid GameStats object");
        }

        String rankSymbol = gs.getRank().toString();
        if ("10".equals(rankSymbol)) {
            rankSymbol = "0"; // Normalize for deck API
        }

        String suitSymbol = gs.getSuit().getSymbol();
        String code = rankSymbol + suitSymbol;

        return fromCode(code);

    }

    public static List<Card> fromCodes(List<String> codes) {
        if (codes == null) {
            return List.of();
        }
        return codes.stream()
                .map(CardUtils::fromCode)
                .collect(Collectors.toList());
    }

    // (Optional bonus) Card → String
    public static String toCode(Card card) {
        return card.getRank() + card.getSuit();
    }

    public static boolean isValidCardFormat(String cardCode) {
        return cardCode != null && cardCode.matches("^[02-9JQKA][HDCS]$");
    }

    public static String requireValidCardFormat(String cardCode) {
        if (cardCode == null || !cardCode.matches("^[02-9JQKA][HDCS]$")) {
            IllegalArgumentException ex = new IllegalArgumentException("Received invalid cardCode: " + cardCode);
            log.error("Invalid card format `" + cardCode + "´", ex);
            throw ex;
        } else {
            return cardCode;
        }
    }

}
