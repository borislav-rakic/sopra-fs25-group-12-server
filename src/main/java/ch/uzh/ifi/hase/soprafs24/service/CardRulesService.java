package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * "Returns cards that the MatchPlayer could play."
     * 
     * @param game
     * @param matchPlayer
     * @return list of playable cards
     **/
    public String getPlayableCardsForMatchPlayerPolling(Game game, MatchPlayer matchPlayer) {
        return getPlayableCardsForMatchPlayer(game, matchPlayer, true);
    }

    /**
     * "Returns cards that the MatchPlayer may play (and is trying to play)."
     * 
     * @param game
     * @param matchPlayer
     * @return list of playable cards
     **/
    public String getPlayableCardsForMatchPlayerPlaying(Game game, MatchPlayer matchPlayer) {
        return getPlayableCardsForMatchPlayer(game, matchPlayer, false);
    }

    /**
     * "Gives assessment of potential candidate cards for playing."
     * 
     * @param game
     * @param matchPlayer
     * @param scenario
     * @return list of playable cards
     **/
    public String getPlayableCardsForMatchPlayer(Game game, MatchPlayer matchPlayer, boolean isPlaying) {
        // Only players who are actually about to play get info on playability of cards.
        if (!game.getPhase().inTrick() || game.getCurrentMatchPlayerSlot() != matchPlayer.getMatchPlayerSlot()) {
            // Only during actual ongoing games does it makes sense to investigate
            // playability of cards (passing must be over).
            return "";
        }

        // Strict first-trick check: force 2C
        if (game.getPhase() == GamePhase.FIRSTTRICK && matchPlayer.hasCardCodeInHand("2C")) {
            return "2C";
        }

        String leadingSuit = game.getSuitOfFirstCardInCurrentTrick();

        log.info("=== Which Cards are Playable? ===");
        log.info(
                "= MatchPlayer [{}] in matchPlayerSlot {}, playing in trick #{}. Trick so far: {}. Leading suit: {}. Phase: {}. Hearts {} broken. Current play order: {}. Hand: [{}]",
                matchPlayer.getInfo(),
                matchPlayer.getMatchPlayerSlot(),
                game.getCurrentTrickNumber(),
                game.getCurrentTrickAsString(),
                leadingSuit,
                game.getPhase(),
                game.getHeartsBroken() ? "is" : "is not yet",
                game.getCurrentPlayOrder(),
                matchPlayer.getHand());

        String hand = matchPlayer.getHand();
        if (hand == null || hand.isBlank()) {
            log.info("=== VERDICT: Hand is empty. ===");
            return "";
        }

        if (leadingSuit == null && matchPlayer.getMatchPlayerSlot() == game.getCurrentMatchPlayerSlot()) {
            log.info("= This player is leading the trick.");

            if (game.getPhase() == GamePhase.FIRSTTRICK) {
                boolean has2C = matchPlayer.hasCardCodeInHand("2C");
                log.info("= It is the first trick and the player in matchPlayerSlot {} with hand {} {} 2C.",
                        matchPlayer.getMatchPlayerSlot(),
                        matchPlayer.getHand(), has2C ? "has" : "does not have");

                if (matchPlayer.getMatchPlayerSlot() == game.getCurrentMatchPlayerSlot() && !has2C && isPlaying) {
                    log.warn("= The card 2C is not found in starting MatchPlayer's hand {}.", matchPlayer.getHand());
                    throw new IllegalStateException(
                            String.format(
                                    "The card 2C is not found in starting MatchPlayer's (matchPlayerSlot %s) hand %s.",
                                    game.getCurrentMatchPlayerSlot(), matchPlayer.getHand()));
                } else if (matchPlayer.getMatchPlayerSlot() == game.getCurrentMatchPlayerSlot()) {
                    return "2C";
                } else {
                    return "2C";
                }
            }

            if (!game.getHeartsBroken()) {
                String nonHearts = matchPlayer.getCardsNotOfSuit('H');
                if (!nonHearts.isBlank()) {
                    log.info("=== VERDICT: Hearts not broken, and the player has non-Hearts cards: [{}]. ===",
                            nonHearts);
                    return CardUtils.normalizeCardCodeString(nonHearts);
                }
            }

            log.info("=== VERDICT: Player only has Hearts or Hearts are broken; may play any card: [{}]. ===", hand);
            return CardUtils.normalizeCardCodeString(hand);
        }

        // Player must follow suit if possible
        String[] cards = hand.split(",");
        log.info(String.format("= The leading suit is [%s].", leadingSuit));
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
            log.info("=== VERDICT: Player has cards in the leading suit and must play one of: [{}]. ===",
                    matchingCards);
            return CardUtils.normalizeCardCodeString(matchingCards.toString());
        } else {
            log.info("==== VERDICT: Player has no cards in the leading suit and may play any card: [{}]. ===", hand);
            return CardUtils.normalizeCardCodeString(hand);
        }
    }

    public void validateMatchPlayerCardCode(Game game, MatchPlayer matchPlayer, String cardCode) {
        log.info("+++ VALIDATEPLAYERCARDCODE: Trick #{} +++", game.getCurrentTrickNumber());
        log.info("+ Game ID: {}, Player: {}, Attempting to play: {}, Hand: {}",
                game.getGameId(), matchPlayer.getInfo(), cardCode, matchPlayer.getHand());

        // 1. Format check
        CardUtils.isValidCardFormat(cardCode);

        // 2. Determine playability
        String playableCards = getPlayableCardsForMatchPlayerPlaying(game, matchPlayer);
        boolean cardIsPlayable = playableCards.contains(cardCode);

        // 3. Check if card is playable
        if (!cardIsPlayable) {
            DebuggingService.richLog(null, matchPlayer.getMatch(), game, matchPlayer, cardCode, "Illegal card played.");

            log.info("+ Only legal cards: {} | Hand: {} | Trick so far: {} | # of Trick: {} | Hearts {}broken",
                    playableCards, matchPlayer.getHand(), game.getCurrentTrick(), game.getCurrentTrickNumber(),
                    game.getHeartsBroken() ? "" : "not ");

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Illegal card played: %s. Legal cards: %s. Current trick: %s",
                            cardCode, playableCards, game.getCurrentTrick()));
        }

        // 4. Confirm the card is in hand
        if (!matchPlayer.hasCardCodeInHand(cardCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("MatchPlayer %s does not hold card %s in their hand.",
                            matchPlayer.getInfo(), cardCode));
        }

        log.info("+++ VERDICT: CARD {} IS OK TO PLAY IN TRICK {} [#{}] +++",
                cardCode, game.getCurrentTrick(), game.getCurrentTrickNumber());
    }

    /**
     * Determines which player (matchPlayerSlot) wins the current trick.
     * Based on highest card of the leading suit.
     *
     * @param game the Game containing current trick state
     * @return the matchPlayerSlot number (integer) of the winning player
     */
    public int determineTrickWinner(Game game) {
        log.info("   ççç DETERMINING TRICK WINNER OF TRICK #{}.", game.getCurrentTrickNumber());
        log.info("   ç Current Trick is {}.", game.getCurrentTrickAsString());
        log.info("   ç Current trick matchPlayerSlot order (derived) is {}.",
                game.getTrickMatchPlayerSlotOrderAsString());

        List<String> trick = game.getCurrentTrick(); // Cards played, in order
        List<Integer> slots = game.getTrickMatchPlayerSlotOrder(); // Slots who played, in order

        if (trick.size() != 4 || slots.size() != 4) {
            throw new IllegalStateException("Cannot determine winner before trick is complete.");
        }

        int leaderSlot = game.getTrickLeaderMatchPlayerSlot();
        int leaderIndex = slots.indexOf(leaderSlot);
        if (leaderIndex == -1) {
            throw new IllegalStateException("Trick leader did not play this trick.");
        }

        List<String> rotatedTrick = rotate(trick, leaderIndex);
        List<Integer> rotatedSlots = rotate(slots, leaderIndex);

        String leadingCardCode = rotatedTrick.get(0);
        char leadingSuit = getSuit(leadingCardCode);
        int bestValue = getCardValue(leadingCardCode);
        int winningIndex = 0;

        for (int i = 1; i < rotatedTrick.size(); i++) {
            String cardCode = rotatedTrick.get(i);
            char suit = getSuit(cardCode);
            int value = getCardValue(cardCode);

            log.info("   ç Evaluating card {} with suit {} and value {}, leading suit: {}",
                    cardCode, suit, value, leadingSuit);
            log.info("   ç MatchPlayerSlot {} played card {}", rotatedSlots.get(i), rotatedTrick.get(i));

            if (suit == leadingSuit && value > bestValue) {
                bestValue = value;
                winningIndex = i;
            }
        }

        int winningSlot = rotatedSlots.get(winningIndex);
        log.info("   ç Establishing Winner: LeaderSlot: {}, Rotated Slots: {}, Trick: {}",
                leaderSlot,
                rotatedSlots.stream().map(String::valueOf).collect(Collectors.joining(",")),
                rotatedTrick);
        log.info("   ççç VERDICT: Winner is {}", winningSlot);
        return winningSlot;
    }

    private <T> List<T> rotate(List<T> list, int index) {
        List<T> result = new ArrayList<>();
        result.addAll(list.subList(index, list.size()));
        result.addAll(list.subList(0, index));
        return result;
    }

    private char getSuit(String cardCode) {
        return cardCode.charAt(cardCode.length() - 1);
    }

    private int getCardValue(String cardCode) {
        String valuePart = cardCode.substring(0, cardCode.length() - 1);
        switch (valuePart) {
            case "J":
                return 11;
            case "Q":
                return 12;
            case "K":
                return 13;
            case "A":
                return 14;
            case "0":
                return 10; // Like our external API, we use '0' for 10 as in '0C'
            default:
                try {
                    return Integer.parseInt(valuePart);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid card code: " + cardCode);
                }
        }
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
     * Returns a message like "Cards will be passed from matchPlayerSlot X to
     * matchPlayerSlot Y
     * (left/right/across/no pass)"
     */
    public String describePassingDirection(int gameNumber, int fromMatchPlayerSlot) {
        Map<Integer, Integer> passMap = determinePassingDirection(gameNumber);
        Integer toMatchPlayerSlot = passMap.get(fromMatchPlayerSlot);

        if (toMatchPlayerSlot == null) {
            return String.format("No passing this round (game %s).", gameNumber);
        }

        String directionLabel;
        switch (gameNumber % 4) {
            case 1 -> directionLabel = "left";
            case 2 -> directionLabel = "across";
            case 3 -> directionLabel = "right";
            default -> directionLabel = "no pass";
        }

        return String.format("Cards will be passed from matchPlayerSlot %s to matchPlayerSlot %s (%s)",
                fromMatchPlayerSlot,
                toMatchPlayerSlot, directionLabel);
    }

}
