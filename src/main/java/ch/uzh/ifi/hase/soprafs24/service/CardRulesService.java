package ch.uzh.ifi.hase.soprafs24.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayerCards;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

@Service
public class CardRulesService {

    private static final int TOTAL_CARD_COUNT = 52;

    /**
     * Returns the list of card codes that the player is allowed to play.
     */
    public List<String> getLegalCards(Game game, MatchPlayer player) {
        List<MatchPlayerCards> hand = player.getCardsInHand();
        if (hand == null || hand.isEmpty()) {
            return List.of();
        }

        // Figure out if player must follow suit based on current trick info in Game
        String leadingSuit = getLeadingSuitOfCurrentTrick(game);

        if (leadingSuit == null) {
            // No cards played yet in this trick → player leads, can play anything
            return hand.stream()
                    .map(MatchPlayerCards::getCard)
                    .collect(Collectors.toList());
        } else {
            // Must follow suit if possible
            List<String> cardsMatchingSuit = hand.stream()
                    .map(MatchPlayerCards::getCard)
                    .filter(card -> card.endsWith(leadingSuit))
                    .collect(Collectors.toList());

            if (!cardsMatchingSuit.isEmpty()) {
                return cardsMatchingSuit;
            } else {
                // No matching suit → can play anything
                return hand.stream()
                        .map(MatchPlayerCards::getCard)
                        .collect(Collectors.toList());
            }
        }
    }

    public List<Card> getLegalCardsAsCards(Game game, MatchPlayer player) {
        List<String> cardCodes = getLegalCards(game, player);
        List<Card> unsortedCards = CardUtils.fromCodes(cardCodes);
        return CardUtils.sortCardsByCardOrder(unsortedCards);
    }

    private void validateSuitFollowRules(Game game, MatchPlayer player, String cardCode) {
        String leadingSuit = getLeadingSuitOfCurrentTrick(game);
        if (leadingSuit == null) {
            return; // No need to follow suit if leading
        }

        // Must follow suit if possible
        boolean hasLeadingSuit = player.getCardsInHand().stream()
                .anyMatch(card -> CardUtils.fromCode(card.getCard()).getSuit().equals(leadingSuit));

        if (hasLeadingSuit) {
            // If player has the leading suit, they must play it
            Card playedCard = CardUtils.fromCode(cardCode);
            if (!playedCard.getSuit().equals(leadingSuit)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must follow suit: " + leadingSuit);
            }
        }
    }

    public boolean isGameFinished(Game game) {
        Match match = game.getMatch();
        List<MatchPlayer> players = match.getMatchPlayers();

        int totalCardsRemaining = 0;
        for (MatchPlayer player : players) {
            if (player.getCardsInHand() != null) {
                totalCardsRemaining += player.getCardsInHand().size();
            }
        }

        return totalCardsRemaining == 0;
    }

    /**
     * Checks whether a given card is legal for the player to play.
     */
    public boolean isCardLegal(Game game, MatchPlayer player, String cardCode) {
        List<String> legalCards = getLegalCards(game, player);
        return legalCards.contains(cardCode);
    }

    /**
     * Helper: Determines the leading suit of the current trick.
     * Purely from Game, not from GameStats.
     */
    private String getLeadingSuitOfCurrentTrick(Game game) {
        if (game.getCurrentTrickSize() == 0) {
            return null; // No cards played yet in this trick
        }

        String firstCard = game.getCardInCurrentTrick(0); // First card played in current trick
        if (firstCard == null || firstCard.length() < 2) {
            throw new IllegalStateException("Invalid card format in trick.");
        }
        return firstCard.substring(firstCard.length() - 1); // Last char = suit
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

    public void validateCardPlay(MatchPlayer matchPlayer, Game game, String cardCode) {
        // 1. Ensure player has the card
        boolean hasCard = matchPlayer.getCardsInHand().stream()
                .anyMatch(card -> card.getCard().equals(cardCode));
        if (!hasCard) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card " + cardCode + " not in hand.");
        }

        // 2. Check first move must be 2C
        if (isFirstCardOfGame(game) && !cardCode.equals("2C")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First card of game must be 2♣.");
        }

        // 3. No hearts or QS on first trick
        if (isFirstTrick(game)) {
            Card card = new Card();
            card.setCode(cardCode);
            if (card.getSuit().equals("H") || cardCode.equals("QS")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot play Hearts or Queen of Spades during first trick.");
            }
        }

        // 4. Validate following suit if needed
        // (you probably already have this inside your old validateCardPlay)
        validateSuitFollowRules(game, matchPlayer, cardCode);
    }

    public Map<Integer, Integer> determinePassingDirection(int gameNumber) {
        Map<Integer, Integer> passMap = new HashMap<>();

        switch (gameNumber % 4) {
            case 1: // Pass to the left
                passMap.put(0, 1);
                passMap.put(1, 2);
                passMap.put(2, 3);
                passMap.put(3, 0);
                break;
            case 2: // Pass to the right
                passMap.put(0, 3);
                passMap.put(1, 0);
                passMap.put(2, 1);
                passMap.put(3, 2);
                break;
            case 3: // Pass across
                passMap.put(0, 2);
                passMap.put(1, 3);
                passMap.put(2, 0);
                passMap.put(3, 1);
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

    private boolean isFirstCardOfGame(Game game) {
        int totalCardsLeft = game.getMatch().getMatchPlayers().stream()
                .mapToInt(mp -> mp.getCardsInHand() != null ? mp.getCardsInHand().size() : 0)
                .sum();
        return totalCardsLeft == 52;
    }

    private boolean isFirstTrick(Game game) {
        int totalCardsLeft = game.getMatch().getMatchPlayers().stream()
                .mapToInt(mp -> mp.getCardsInHand() != null ? mp.getCardsInHand().size() : 0)
                .sum();
        return totalCardsLeft > 48; // 52 - 4
    }
}
