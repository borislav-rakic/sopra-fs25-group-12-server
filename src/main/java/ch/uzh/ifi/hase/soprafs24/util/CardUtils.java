package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.model.Card;

import java.util.Arrays;
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
        return cardCode != null && cardCode.matches(GameConstants.CARD_CODE_REGEX);
    }

    public static String requireValidCardFormat(String cardCode) {
        if (cardCode == null || !cardCode.matches(GameConstants.CARD_CODE_REGEX)) {
            IllegalArgumentException ex = new IllegalArgumentException("Received invalid cardCode: " + cardCode);
            log.error("Invalid card format `" + cardCode + "´", ex);
            throw ex;
        } else {
            return cardCode;
        }
    }

    public static String normalizeCardCodeString(String cardCodes) {
        if (cardCodes == null || cardCodes.isBlank()) {
            return "";
        }

        List<String> codes = List.of(cardCodes.split(","));

        // Validate all codes first
        for (String code : codes) {
            requireValidCardFormat(code.trim()); // throws if invalid
        }

        // Sort and return
        List<String> sorted = codes.stream()
                .map(String::trim)
                .sorted(CardUtils::compareCards)
                .collect(Collectors.toList());

        return String.join(",", sorted);
    }

    /**
     * Calculates a sortable numeric value for a card code.
     * Based on "score". First QS, then all Hearts, then descending Value.
     */
    public static int calculateHighestScoreOrder(String cardCode) {
        if (cardCode == null || cardCode.length() < 2 || cardCode.length() > 3) {
            throw new IllegalArgumentException("Invalid card format: " + cardCode);
        }

        // Queen of Spades
        // QS = 999.
        if (cardCode.equals("QS")) {
            return 999;
        }

        String rankStr = cardCode.substring(0, cardCode.length() - 1);
        char suitChar = cardCode.charAt(cardCode.length() - 1);

        // Any Hearts
        // AH = 914, KH = 913, ..., 2H = 902.
        if (suitChar == 'H') {
            int suitInt = rankStrToInt(rankStr);
            return 900 + suitInt;
        }

        int suitValue = switch (suitChar) {
            case 'C' -> 0;
            case 'D' -> 15;
            case 'S' -> 30;
            default -> throw new IllegalArgumentException("Invalid suit: " + suitChar);
        };
        int rankValue = rankStrToInt(rankStr);

        // AS = 730, AD = 715, AC = 700
        // KS = 680, KD = 665, KC = 650
        // QS = 630, QD = 615, QC = 600
        // ...
        // 3S = 180, 3D = 165, 3C = 150
        // 2S = 130, 2D = 115, 2C = 100

        return rankValue * 50 + suitValue;
    }

    public static int rankStrToInt(String rankStr) {
        return switch (rankStr) {
            case "J" -> 11;
            case "Q" -> 12;
            case "K" -> 13;
            case "A" -> 14;
            case "10", "0" -> 10;
            default -> {
                if (rankStr.matches("[2-9]")) {
                    yield Integer.parseInt(rankStr);
                }
                yield 0;
            }
        };
    }

    public static int suitCharToInt(char suitChar) {
        int suitValue = switch (suitChar) {
            case 'C' -> 10;
            case 'D' -> 30;
            case 'S' -> 50;
            case 'H' -> 70;
            default -> throw new IllegalArgumentException("Invalid suit: " + suitChar);
        };
        return suitValue;
    }

    public static List<String> requireSplitCardCodesAsListOfStrings(String cardCodes) {
        if (cardCodes == null || cardCodes.isBlank()) {
            return List.of(); // Empty hand or input
        }

        return Arrays.stream(cardCodes.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .peek(CardUtils::requireValidCardFormat) // Throws if invalid
                .collect(Collectors.toList());
    }

    public static List<String> splitCardCodesAsListOfStrings(String cardCodes) {
        if (cardCodes == null || cardCodes.isBlank()) {
            return List.of();
        }

        return Arrays.stream(cardCodes.split(","))
                .map(String::trim)
                .filter(CardUtils::isValidCardFormat) // Filters invalid ones
                .collect(Collectors.toList());
    }

    public static String joinValidatedCardCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return "";
        }

        return codes.stream()
                .map(String::trim)
                .peek(CardUtils::requireValidCardFormat) // Validate each card
                .sorted(CardUtils::compareCards) // Sort by game order
                .collect(Collectors.joining(","));
    }

    public static String joinCardCodesSkipSilently(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return "";
        }

        return codes.stream()
                .map(String::trim)
                .filter(CardUtils::isValidCardFormat)
                .sorted(CardUtils::compareCards)
                .collect(Collectors.joining(","));
    }

}
