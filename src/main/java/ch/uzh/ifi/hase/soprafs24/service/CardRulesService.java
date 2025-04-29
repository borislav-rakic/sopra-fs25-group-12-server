package ch.uzh.ifi.hase.soprafs24.service;

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

@Service
public class CardRulesService {

    private static final int TOTAL_CARD_COUNT = 52;

    public String getPlayableCardsForMatchPlayer(MatchPlayer matchPlayer) {
        String hand = matchPlayer.getHand();
        if (hand == null || hand.isBlank()) {
            return "";
        }

        Match match = matchPlayer.getMatch();
        Game activeGame = match.getActiveGame();
        if (activeGame == null) {
            throw new IllegalStateException("No active game found for this match.");
        }

        String leadingSuit = getLeadingSuitOfCurrentTrick(activeGame);

        if (leadingSuit == null) {
            if (activeGame.getPhase() == GamePhase.FIRSTTRICK) {
                if (!matchPlayer.hasCardCodeInHand("2C")) {
                    throw new IllegalStateException("2C is not found in starting MatchPlayer's hand.");
                }
                return "2C";
            }
            if (!activeGame.getHeartsBroken()) {
                return matchPlayer.getCardsNotOfSuit('H');
            }
            return hand;
        } else {
            // Filter cards matching leading suit
            StringBuilder matchingCards = new StringBuilder();
            String[] cards = hand.split(",");

            for (String card : cards) {
                if (card.endsWith(leadingSuit)) {
                    if (matchingCards.length() > 0) {
                        matchingCards.append(",");
                    }
                    matchingCards.append(card);
                }
            }

            if (matchingCards.length() > 0) {
                return matchingCards.toString();
            } else {
                // No matching suit -> can play any card
                return hand;
            }
        }
    }

    public void validatePlayedCard(MatchPlayer matchPlayer, String cardCode) {
        CardUtils.isValidCardFormat(cardCode);
        if (!getPlayableCardsForMatchPlayer(matchPlayer).contains(cardCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illegal card played: " + cardCode);
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

}
