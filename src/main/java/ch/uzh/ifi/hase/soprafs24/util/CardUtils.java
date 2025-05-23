package ch.uzh.ifi.hase.soprafs24.util;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.model.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import ch.uzh.ifi.hase.soprafs24.model.CardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardUtils {
    private static final Logger log = LoggerFactory.getLogger(CardUtils.class);

    private static final List<String> EXPECTED_CARD_CODES = new ArrayList<>(Arrays.asList(
            "2C", "3C", "4C", "5C", "6C", "7C", "8C", "9C", "0C", "JC", "QC", "KC", "AC",
            "2D", "3D", "4D", "5D", "6D", "7D", "8D", "9D", "0D", "JD", "QD", "KD", "AD",
            "2H", "3H", "4H", "5H", "6H", "7H", "8H", "9H", "0H", "JH", "QH", "KH", "AH",
            "2S", "3S", "4S", "5S", "6S", "7S", "8S", "9S", "0S", "JS", "QS", "KS", "AS"));

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
            case "0" -> 10;
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
            log.debug("Invalid card format `" + cardCode + "´", ex);
            throw ex;
        } else {
            return cardCode;
        }
    }

    public static String normalizeCardCodeString(String cardCodes) {
        if (cardCodes == null || cardCodes.isBlank()) {
            return "";
        }

        return Arrays.stream(cardCodes.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .peek(CardUtils::isValidCardFormat) // Silently skips doubles.
                .distinct() // Remove duplicates
                .sorted(CardUtils::compareCards) // Sort by card order
                .collect(Collectors.joining(","));
    }

    public static int sizeOfCardCodeString(String cardCodes) {
        if (cardCodes == null || cardCodes.isBlank()) {
            return 0;
        }

        List<String> codes = List.of(cardCodes.split(","));

        // Validate all codes first
        for (String code : codes) {
            requireValidCardFormat(code.trim()); // throws if invalid
        }

        int numberOfItems = codes.size();
        return numberOfItems;
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
        if (GameConstants.QUEEN_OF_SPADES.equals(cardCode)) {
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
            case "0" -> 10;
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

    public static boolean isCardCodeInHandAsString(String cardCode, String handAsString) {
        if (cardCode == null || handAsString == null || cardCode.length() != 2) {
            return false;
        }

        List<String> cardCodes = CardUtils.splitCardCodesAsListOfStrings(handAsString);
        return cardCodes.contains(cardCode);
    }

    public static int numberOfHeartsCardInCardCodeString(String cardCodeString) {
        if (cardCodeString == null || cardCodeString.isBlank()) {
            return 0;
        }

        return (int) splitCardCodesAsListOfStrings(cardCodeString).stream()
                .filter(code -> code.length() >= 2 && code.charAt(code.length() - 1) == 'H')
                .count();
    }

    public static int numberOfNonHeartsCardsInCardCodeString(String cardCodeString) {
        if (cardCodeString == null || cardCodeString.isBlank()) {
            return 0;
        }

        return (int) splitCardCodesAsListOfStrings(cardCodeString).stream()
                .filter(code -> code.length() >= 2 && code.charAt(code.length() - 1) != 'H')
                .count();
    }

    public static String getSuitOfFirstCardInCardCodeString(String cardCodeString) {
        if (cardCodeString == null || cardCodeString.isBlank()) {
            return "";
        }

        return splitCardCodesAsListOfStrings(cardCodeString).stream()
                .findFirst()
                .map(code -> String.valueOf(code.charAt(code.length() - 1)))
                .orElse("");
    }

    public static boolean isSuitInHand(String cardCodeString, String suit) {
        if (cardCodeString == null || cardCodeString.isBlank()) {
            return false;
        }

        if (suit == null || suit.isBlank()) {
            return false;
        }

        List<String> cardCodes = splitCardCodesAsListOfStrings(cardCodeString);

        return cardCodes.stream()
                .anyMatch(code -> code.length() >= 2 &&
                        String.valueOf(code.charAt(code.length() - 1)).equalsIgnoreCase(suit));
    }

    public static String cardCodesInSuit(String cardCodeString, String suit) {
        if (cardCodeString == null || cardCodeString.isBlank()) {
            return "";
        }

        List<String> matchingCards;

        if (suit == null || suit.isBlank()) {
            matchingCards = splitCardCodesAsListOfStrings(cardCodeString);
        } else {
            matchingCards = splitCardCodesAsListOfStrings(cardCodeString).stream()
                    .filter(code -> code.length() >= 2 &&
                            String.valueOf(code.charAt(code.length() - 1)).equalsIgnoreCase(suit))
                    .collect(Collectors.toList());
        }

        if (matchingCards.isEmpty()) {
            return "";
        }

        return matchingCards.stream()
                .sorted(CardUtils::compareCards)
                .collect(Collectors.joining(","));
    }

    public static String cardCodesNotInSuit(String cardCodeString, String suit) {
        if (cardCodeString == null || cardCodeString.isBlank()) {
            return "";
        }

        List<String> remainingCards;

        if (suit == null || suit.isBlank()) {
            remainingCards = splitCardCodesAsListOfStrings(cardCodeString);
        } else {
            remainingCards = splitCardCodesAsListOfStrings(cardCodeString).stream()
                    .filter(code -> code.length() >= 2 &&
                            !String.valueOf(code.charAt(code.length() - 1)).equalsIgnoreCase(suit))
                    .collect(Collectors.toList());
        }

        if (remainingCards.isEmpty()) {
            return "";
        }

        return remainingCards.stream()
                .sorted(CardUtils::compareCards)
                .collect(Collectors.joining(","));
    }

    public static String cardCodeStringMinusCardCode(String cardCodeString, String cardCodeToRemove) {
        if (cardCodeString == null || cardCodeString.isBlank()) {
            return "";
        }

        if (cardCodeToRemove == null || cardCodeToRemove.isBlank()) {
            return normalizeCardCodeString(cardCodeString); // nothing to remove
        }

        String cardToRemove = cardCodeToRemove.trim();

        List<String> remainingCards = splitCardCodesAsListOfStrings(cardCodeString).stream()
                .map(String::trim)
                .filter(code -> !code.equals(cardToRemove))
                .distinct()
                .sorted(CardUtils::compareCards)
                .collect(Collectors.toList());

        if (remainingCards.isEmpty()) {
            return "";
        }

        return String.join(",", remainingCards);
    }

    public static boolean validateDrawnCards(List<CardResponse> cards) {
        // Check size
        if (cards == null || cards.size() != GameConstants.FULL_DECK_CARD_COUNT) {
            return false;
        }

        // Keep track of seen card codes
        List<String> seenCodes = new ArrayList<>();

        // Iterate and check each card's code
        for (CardResponse card : cards) {
            String code = card.getCode();

            // Code should not be null
            if (code == null || code.trim().isEmpty()) {
                return false;
            }

            // Check if the code is one of the expected card codes
            if (!EXPECTED_CARD_CODES.contains(code)) {
                return false;
            }

            // Check if code was already seen
            if (seenCodes.contains(code)) {
                return false;
            }

            seenCodes.add(code);
        }

        return seenCodes.size() == GameConstants.FULL_DECK_CARD_COUNT;
    }

    private static final String[] RANKS = { "2", "3", "4", "5", "6", "7", "8", "9", "0", "J", "Q", "K", "A" };
    private static final String[] SUITS = { "C", "D", "S", "H" }; // Hearts, Diamonds, Clubs, Spades

    public static List<String> getFullDeckList() {
        List<String> deck = new ArrayList<>();
        for (String rank : RANKS) {
            for (String suit : SUITS) {
                deck.add(rank + suit);
            }
        }
        return deck;
    }

    /**
     * Utility to evenly distribute all 52 cards into hands for 4 players.
     * Sets 13 cards per player in their `hand` field, clears takenCards.
     */
    public static List<MatchPlayer> generateBalancedMatchPlayersWithFullDeck() {
        List<String> deck = getFullDeckList();
        Collections.shuffle(deck);

        List<MatchPlayer> players = new ArrayList<>();
        int cardsPerPlayer = 13;

        for (int i = 0; i < 4; i++) {
            MatchPlayer mp = new MatchPlayer();
            mp.setMatchPlayerSlot(i + 1);
            List<String> handCards = deck.subList(i * cardsPerPlayer, (i + 1) * cardsPerPlayer);
            mp.setHand(String.join(",", handCards));
            mp.setTakenCards(new ArrayList<>());
            mp.setGameScore(0);
            players.add(mp);
        }

        return players;
    }
}
