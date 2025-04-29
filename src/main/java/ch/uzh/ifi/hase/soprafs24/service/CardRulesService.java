package ch.uzh.ifi.hase.soprafs24.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import ch.uzh.ifi.hase.soprafs24.util.DebuggingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CardRulesService {

    private final Logger log = LoggerFactory.getLogger(CardRulesService.class);

    private static final int TOTAL_CARD_COUNT = 52;

    public String getPlayableCardsForMatchPlayer(Game game, MatchPlayer matchPlayer) {
        String myReturn = "";
        String leadingSuit = getLeadingSuitOfCurrentTrick(game);

        log.info("=== Which Cards are Playable? ===");
        log.info(
                "MatchPlayer [{}] in slot {}, playing in trick #{}. Trick so far: {}. Leading suit: {}. Phase: {}. Hearts {} broken. Current play order: {}. Hand: [{}]",
                matchPlayer.getInfo(),
                matchPlayer.getSlot(),
                game.getCurrentTrickNumber(),
                game.getCurrentTrick(),
                leadingSuit,
                game.getPhase(),
                game.getHeartsBroken() ? "is" : "is not yet",
                game.getCurrentPlayOrder(),
                matchPlayer.getHand());

        String hand = matchPlayer.getHand();
        if (hand == null || hand.isBlank()) {
            log.info("VERDICT: Hand is empty.");
            return "";
        }

        if (leadingSuit == null) {
            log.info("This player is leading the trick.");

            if (game.getPhase() == GamePhase.FIRSTTRICK) {
                boolean has2C = matchPlayer.hasCardCodeInHand("2C");
                log.info("It is the first trick and the player {} 2C.", has2C ? "has" : "does not have");

                if (!has2C) {
                    throw new IllegalStateException("The card 2C is not found in starting MatchPlayer's hand.");
                }

                log.info("VERDICT: Only the card 2C is allowed as the opening play: [2C]");
                return "2C";
            }

            if (!game.getHeartsBroken()) {
                String nonHearts = matchPlayer.getCardsNotOfSuit('H');
                if (!nonHearts.isBlank()) {
                    log.info("VERDICT: Hearts not broken, and the player has non-Hearts cards: [{}]",
                            nonHearts);
                    return nonHearts;
                }
            }

            log.info("VERDICT: Player only has Hearts or Hearts are broken; may play any card: [{}]", hand);
            return hand;
        }

        // Player must follow suit if possible
        String[] cards = hand.split(",");
        log.info(String.format("The leading suit is [%s].", leadingSuit));
        StringBuilder matchingCards = new StringBuilder();
        for (String card : cards) {
            if (!card.isBlank() && card.endsWith(leadingSuit)) {
                if (matchingCards.length() > 0) {
                    matchingCards.append(",");
                }
                matchingCards.append(card);
            }
        }

        if (matchingCards.length() > 0) {
            log.info("VERDICT Player has cards in the leading suit and must play one of: [{}]", matchingCards);
            return matchingCards.toString();
        } else {
            log.info("Player has no cards in the leading suit and may play any card: [{}]", hand);
            return hand;
        }
    }

    public void validateMatchPlayerCardCode(Game game, MatchPlayer matchPlayer, String cardCode) {
        log.warn("🔥 validateMatchPlayerCardCode CALLED 🔥");
        Arrays.stream(Thread.currentThread().getStackTrace())
                .forEach(ste -> log.warn("  at {}", ste));
        log.info("THIS IS VALIDATEPLAYERCARDCODE and currentTrickNumber is {}", game.getCurrentTrickNumber());
        game.setCurrentTrickNumber(game.getCurrentTrickNumber() + 1);
        log.info(
                "This is game {} and MatchPlayer {} is trying to play the card {}, their hand being: {}.",
                game.getGameId(), matchPlayer.getInfo(), cardCode, matchPlayer.getHand());
        // 1. This is a valid cardCode.
        CardUtils.isValidCardFormat(cardCode);
        if (!getPlayableCardsForMatchPlayer(game, matchPlayer).contains(cardCode)) {
            DebuggingService.richLog(null, matchPlayer.getMatch(), game, matchPlayer, cardCode, "Illegal card played.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illegal card played: " + cardCode
                    + ". Legal cards are said to be: " + getPlayableCardsForMatchPlayer(game, matchPlayer));

        }
        // 2. The player actually holds that card
        boolean matchPlayerHoldsCard = matchPlayer.hasCardCodeInHand(cardCode);
        if (!matchPlayerHoldsCard) {
            String informativeThrow = String.format("MatchPlayer %s does not hold card %s in their hand.",
                    matchPlayer.getInfo(), cardCode);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, informativeThrow);
        }
        // 3. This card is among the legal moves in this game.
        String playableCards = getPlayableCardsForMatchPlayer(game, matchPlayer);
        boolean isPlayable = isCardCodePlayable(cardCode, playableCards);
        if (!isPlayable) {
            String informativeThrow = String.format("MatchPlayer %s cannot play card %s.",
                    matchPlayer.getInfo(), cardCode);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, informativeThrow);
        }
    }

    private String getLeadingSuitOfCurrentTrick(Game game) {
        if (game.getCurrentTrickSize() == 0)
            return null;
        String firstCard = game.getCardInCurrentTrick(0);
        return firstCard.substring(firstCard.length() - 1);
    }

    /**
     * Helper: Returns the cardCode of the leading card of the trick.
     * 
     * @param game : current game.
     * @return card code.
     */
    private String getLeadingCardCodeOfCurrentTrick(Game game) {
        if (game.getCurrentTrickSize() == 0) {
            return null; // No cards played yet in this trick
        }
        // First card played in current trick
        String firstCard = game.getCardInCurrentTrick(0);
        if (firstCard == null || firstCard.length() < 2) {
            throw new IllegalStateException("Invalid card format in trick.");
        }
        // Last char = suit
        return firstCard.substring(firstCard.length() - 1);
    }

    /**
     * Determines which player (slot) wins the current trick.
     * Based on highest card of the leading suit.
     *
     * @param game the Game containing current trick state
     * @return the slot number (integer) of the winning player
     */
    public int determineTrickWinner(Game game) {
        List<String> currentTrick = game.getCurrentTrick();
        List<Integer> currentTrickSlots = game.getCurrentTrickSlots();

        if (currentTrick == null || currentTrickSlots == null || currentTrick.size() != 4
                || currentTrickSlots.size() != 4) {
            throw new IllegalStateException("Cannot determine winner before trick is complete.");
        }

        // Leading card determines leading suit
        Card leadingCard = CardUtils.fromCode(currentTrick.get(0));
        String leadingSuit = leadingCard.getSuit(); // e.g., "H", "S", "C", "D"

        int bestIndex = 0;
        int bestValue = leadingCard.getValue();

        for (int i = 1; i < currentTrick.size(); i++) {
            Card currentCard = CardUtils.fromCode(currentTrick.get(i));

            // Only compare cards of the same suit
            if (leadingSuit.equals(currentCard.getSuit())) {
                if (currentCard.getValue() > bestValue) {
                    bestValue = currentCard.getValue();
                    bestIndex = i;
                }
            }
        }

        return currentTrickSlots.get(bestIndex);
    }

    /**
     * Calculates the total points for a finished trick based on card codes.
     * Hearts are 1 point each, Queen of Spades is 13 points.
     */
    public int calculateTrickPoints(List<String> finishedTrick) {
        if (finishedTrick == null || finishedTrick.isEmpty()) {
            return 0;
        }

        int points = 0;

        for (String cardCode : finishedTrick) {
            Card card = CardUtils.fromCode(cardCode);
            String suit = card.getSuit(); // "H", "D", "S", "C"
            String rank = card.getRank(); // "2", "3", ..., "Q", "K", "A", "10"

            if ("H".equals(suit)) {
                points += 1; // Each Heart is 1 point
            } else if ("S".equals(suit) && "Q".equals(rank)) {
                points += 13; // Queen of Spades is 13 points
            }
        }

        return points;
    }

    public Map<Integer, Integer> determinePassingDirection(int gameNumber) {
        Map<Integer, Integer> passMap = new HashMap<>();

        switch (gameNumber % 4) {
            case 1: // Pass to the left
                passMap.put(1, 2);
                passMap.put(2, 3);
                passMap.put(3, 4);
                passMap.put(4, 1);
                break;
            case 2: // Pass across
                passMap.put(1, 3);
                passMap.put(2, 4);
                passMap.put(3, 1);
                passMap.put(4, 2);
                break;
            case 3: // Pass to the right
                passMap.put(1, 4);
                passMap.put(2, 1);
                passMap.put(3, 2);
                passMap.put(4, 3);
                break;
            default: // No pass
                break;
        }

        return passMap;
    }

    public void ensureHeartBreak(Game game) {
        // Assumes that playing the card was legal!
        if (game.getHeartsBroken()) {
            return; // Already broken, nothing to do
        }

        List<String> currentTrick = game.getCurrentTrick();
        if (currentTrick == null || currentTrick.isEmpty()) {
            return; // No cards yet
        }

        String lastCardCode = currentTrick.get(currentTrick.size() - 1);
        Card lastCard = CardUtils.fromCode(lastCardCode);

        if ("H".equals(lastCard.getSuit())) {
            game.setHeartsBroken(true);
        }
    }

    public boolean isGameReadyForResults(Game game) {
        Match match = game.getMatch();
        if (match == null) {
            throw new IllegalStateException("Game has no associated match.");
        }

        int totalCardsRemaining = 0;
        for (MatchPlayer player : match.getMatchPlayers()) {
            String hand = player.getHand();
            if (hand != null && !hand.isBlank()) {
                totalCardsRemaining += hand.split(",").length;
            }
        }

        return totalCardsRemaining == 0;
    }

    public static boolean isCardCodePlayable(String cardCode, String playableCardsCsv) {
        return Arrays.asList(playableCardsCsv.split(",")).contains(cardCode);
    }

    /**
     * Returns a message like "Cards will be passed from slot X to slot Y
     * (left/right/across/no pass)"
     */
    public String describePassingDirection(int gameNumber, int fromSlot) {
        Map<Integer, Integer> passMap = determinePassingDirection(gameNumber);
        Integer toSlot = passMap.get(fromSlot);

        if (toSlot == null) {
            return String.format("No passing this round (game %s).", gameNumber);
        }

        String directionLabel;
        switch (gameNumber % 4) {
            case 1 -> directionLabel = "left";
            case 2 -> directionLabel = "across";
            case 3 -> directionLabel = "right";
            default -> directionLabel = "no pass";
        }

        return String.format("Cards will be passed from slot %s to slot %s (%s)", fromSlot, toSlot, directionLabel);
    }

}
