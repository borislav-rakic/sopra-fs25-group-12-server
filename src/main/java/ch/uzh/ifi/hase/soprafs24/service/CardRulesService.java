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

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
import ch.uzh.ifi.hase.soprafs24.model.Card;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import ch.uzh.ifi.hase.soprafs24.util.DebuggingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CardRulesService {

    private final Logger log = LoggerFactory.getLogger(CardRulesService.class);

    private final GameStatsService gameStatsService;

    public CardRulesService(GameStatsService gameStatsService) {
        this.gameStatsService = gameStatsService;
    }

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
        // Game state is assumed to be perfectly consistent!
        // log.info(" ¦¦¦ REACHED getPlayableCardsForMatchPlayer ¦¦¦");

        // Only players who are actually about to play get info on playability of cards.
        if (!game.getPhase().inTrick()
                || game.getCurrentMatchPlayerSlot() != matchPlayer.getMatchPlayerSlot()) {
            // Only during actual ongoing games does it makes sense to investigate
            // playability of cards (passing must be over).

            // log.info(" ¦ VERDICT: Out of turn.");
            return "";
        }
        // A. Player's Hand
        String hand = CardUtils.normalizeCardCodeString(matchPlayer.getHand());
        // log.info(" ¦ hand: {}.", hand);
        int handSize = CardUtils.sizeOfCardCodeString(hand);
        // log.info(" ¦ handSize: {}.", handSize);

        boolean handHas2C = CardUtils.isCardCodeInHandAsString(GameConstants.TWO_OF_CLUBS, hand);
        // log.info(" ¦ handHas2C: {}.", handHas2C);
        String handHearts = CardUtils.cardCodesInSuit(hand, "H");
        // log.info(" ¦ heartsCards: {}.", handHearts);
        String handNonHearts = CardUtils.cardCodesNotInSuit(hand, "H");
        // log.info(" ¦ handNonHearts: {}.", handNonHearts);
        int handNonHeartsSize = CardUtils.sizeOfCardCodeString(handNonHearts);
        // log.info(" ¦ handNonHeartsSize: {}.", handNonHeartsSize);

        // B. The Trick so far
        String trick = game.getCurrentTrickAsString();
        // log.info(" ¦ trick: {}.", trick);
        int trickSize = CardUtils.sizeOfCardCodeString(trick);
        // log.info(" ¦ trickSize: {}.", trickSize);
        String leadingSuit = CardUtils.getSuitOfFirstCardInCardCodeString(trick);
        // log.info(" ¦ leadingSuit: {}.", leadingSuit);

        boolean handHasLeadingSuit = CardUtils.isSuitInHand(hand, leadingSuit);
        // log.info(" ¦ handHasLeadingSuit: {}.", handHasLeadingSuit);
        String handLeadingSuit = CardUtils.cardCodesInSuit(hand, leadingSuit);
        // log.info(" ¦ handLeadingSuit: {}.", handLeadingSuit);
        int handLeadingSuitSize = CardUtils.sizeOfCardCodeString(handLeadingSuit);
        // log.info(" ¦ handLeadingSuitSize: {}.", handLeadingSuitSize);

        String handLeadingSuitNonQS = CardUtils.cardCodeStringMinusCardCode(handLeadingSuit,
                GameConstants.QUEEN_OF_SPADES);
        // log.info(" ¦ handLeadingSuitNonQS: {}.", handLeadingSuitNonQS);

        String handNonHeartsNonQS = CardUtils.cardCodeStringMinusCardCode(handNonHearts,
                GameConstants.QUEEN_OF_SPADES);
        int handNonHeartsNonQSSize = CardUtils.sizeOfCardCodeString(handNonHeartsNonQS);
        // log.info(" ¦ handNonHeartsNonQSSize: {}.", handNonHeartsNonQSSize);

        // C. The Game state
        boolean heartsBroken = game.getHeartsBroken();
        // log.info(" ¦ heartsBroken: {}.", heartsBroken);
        int playOrder = game.getCurrentPlayOrder();
        // log.info(" ¦ playOrder: {}.", playOrder);
        int trickNumber = game.getCurrentTrickNumber();
        // log.info(" ¦ trickNumber: {}.", trickNumber);
        GamePhase gamePhase = game.getPhase();
        // log.info(" ¦ gamePhase: {}.", gamePhase);

        // log.info(" ¦ Which Cards are Playable?");
        // log.info(
        // " ¦ MatchPlayer {} in matchPlayerSlot {}, playing in trick #{}.",
        // matchPlayer.getInfo(),
        // matchPlayer.getMatchPlayerSlot(),
        // trickNumber);
        // log.info("Trick so far: {}. Leading suit: {}. Phase: {}. Hearts {} broken.
        // Current play order: {}.",
        // trick,
        // leadingSuit,
        // gamePhase,
        // heartsBroken ? "is" : "is not yet",
        // playOrder,
        // hand);

        // ### FACTS ESTABLISHED

        String playableCards = "";
        // Here goes...
        // Case 0. There are no cards in the player's hand
        if (handSize == 0) {
            // log.info(" ¦ Case 0.");
            throw new GameplayException(
                    String.format(
                            "Player has no cards in his hands: %s.",
                            hand));
        }
        // ESTABLISHED: There are cards in the player's hands
        // Case 1a. It is the first card of the firsttrick and handHas 2C.
        else if (gamePhase == GamePhase.FIRSTTRICK && playOrder == 0 && handHas2C) {
            // log.info(" ¦ Case 1a.");
            playableCards = GameConstants.TWO_OF_CLUBS;
        }
        // Case 1b. It is NOT the first card of the firsttrick and handHas 2C => Illegal
        // state.
        else if (playOrder != 0 && handHas2C) {
            // log.info(" ¦ Case 1b.");
            throw new IllegalStateException(
                    String.format(
                            "Player holds card 2C (hand=%s), even though currenPlayOrder is %d.",
                            hand,
                            playOrder));
        }
        // ESTABLISHED: It is NOT the first card of the first trick.
        // ESTABLISHED: The player does NOT hold "2C".
        // Case 2a. It is the first trick, but the leading suit is Hearts => illegal
        // state.
        else if (gamePhase == GamePhase.FIRSTTRICK && "H".equals(leadingSuit)) {
            // log.info(" ¦ Case 2a.");
            throw new IllegalStateException(
                    String.format(
                            "In the first trick, the leading suit must not be Hearts."));
        }
        // Case 2b. Player has the leading suit of the first round (no QS!)
        else if (gamePhase == GamePhase.FIRSTTRICK && trickSize > 1 && handLeadingSuitSize > 0) {
            // The Queen of Spades is illegal in the first trick
            // log.info(" ¦ Case2 b.");
            playableCards = handLeadingSuitNonQS;
        }
        // Case 2c. Player does not have the leading suit of the first trick,
        // but some other non-heart suit (still no QS!)
        else if (gamePhase == GamePhase.FIRSTTRICK
                && trickSize > 1
                && handLeadingSuitSize == 0
                && handNonHeartsNonQSSize > 0) {
            // The Queen of Spades is illegal in the first trick
            // log.info(" ¦ Case 2c.");
            playableCards = handNonHeartsNonQS;
        }
        // Case 2d. Player does not have the leading suit of the first trick,
        // and neither any other non-heart suit (still no QS!)
        // so hearts are actually allowed!
        else if (gamePhase == GamePhase.FIRSTTRICK
                && trickSize > 1
                && handLeadingSuitSize == 0
                && handNonHeartsNonQSSize == 0) {
            // The Queen of Spades is illegal in the first trick
            // log.info(" ¦ Case 2d.");
            playableCards = handHearts;
        }
        // ESTABLISHED: It is NOT the first round.
        // Case 3a. The trick is empty, but Hearts is not yet broken, but player only
        // has Hearts cards.
        else if (trickSize == 0 && !heartsBroken && handNonHeartsSize == 0) {
            // log.info(" ¦ Case 3a.");
            playableCards = handHearts;
        }
        // Case 3b. The trick is empty, but Hearts is not yet broken.
        else if (trickSize == 0 && !heartsBroken) {
            // log.info(" ¦ Case 3b.");
            playableCards = handNonHearts;
        }
        // Case 3c. The trick is empty, but Hearts IS broken.
        else if (trickSize == 0 && heartsBroken) {
            // log.info(" ¦ Case 3c.");
            playableCards = hand;
        }
        // ESTABLISHED: The Trick is NOT empty.
        // Case 4a. The trick is NOT empty and player has cards in leading suit.
        else if (trickSize > 0 && handHasLeadingSuit) {
            // log.info(" ¦ Case 4a.");
            playableCards = handLeadingSuit;
        }
        // Case 4b. The trick is NOT empty and player has no cards in leading suit.
        else if (trickSize > 0 && !handHasLeadingSuit) {
            // log.info(" ¦ Case 4b.");
            playableCards = hand;
        } else {
            // log.info(" ¦ Case 9.");
            throw new IllegalStateException(
                    String.format(
                            "Player's cards did not fit any category, really."));

        }
        log.info(
                "   ¦ Playable cards (hand: [{}]) card #{} in trick #{}; cards in trick: [{}] ({}) and hearts are {}broken: {}. Verdict: [{}].",
                hand,
                trickSize + 1,
                trickNumber,
                trick,
                leadingSuit.isEmpty() ? "no leading suit" : "leading suit:" + leadingSuit,
                heartsBroken ? "" : "not ",
                playableCards);
        // log.info(" ¦¦¦ VERDICT: {}.", playableCards);
        return CardUtils.normalizeCardCodeString(playableCards);

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
        // log.info(" ççç DETERMINING TRICK WINNER OF TRICK #{}.",
        // game.getCurrentTrickNumber());
        // log.info(" ç Current Trick is {}.", game.getCurrentTrickAsString());

        List<String> trick = game.getCurrentTrick(); // Cards played, in order
        List<Integer> slots = game.getTrickMatchPlayerSlotOrder(); // Slots who played, in order

        if (trick.size() != GameConstants.MAX_TRICK_SIZE || slots.size() != GameConstants.MAX_TRICK_SIZE) {
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

            // log.info(" ç Evaluating card {} with suit {} and value {}, leading suit: {}",
            // cardCode, suit, value, leadingSuit);
            // log.info(" ç MatchPlayerSlot {} played card {}", rotatedSlots.get(i),
            // rotatedTrick.get(i));

            if (suit == leadingSuit && value > bestValue) {
                bestValue = value;
                winningIndex = i;
            }
        }

        int winningSlot = rotatedSlots.get(winningIndex);
        // log.info(" ç Establishing Winner: LeaderSlot: {}, Rotated Slots: {}, Trick:
        // {}",
        // leaderSlot,
        // rotatedSlots.stream().map(String::valueOf).collect(Collectors.joining(",")),
        // rotatedTrick);
        // log.info(" ççç VERDICT: Winner is {}", winningSlot);
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
     * Calculates the total points for a finished trick based on card codes, and
     * updates the GAME_STATS relation.
     * Hearts are 1 point each, Queen of Spades is 13 points.
     */
    public int calculateTrickPoints(Game game, int winnerMatchPlayerSlot) {
        List<String> finishedTrick = game.getCurrentTrick();

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

            // Updates the points_billed_to column in the GAME_STATS relation
            gameStatsService.updateGameStatsPointsBilledTo(game, cardCode, winnerMatchPlayerSlot);
        }

        return points;
    }

    public Map<Integer, Integer> determinePassingDirection(int gameNumber) {
        Map<Integer, Integer> passMap = new HashMap<>();

        switch (gameNumber % GameConstants.MAX_TRICK_SIZE) {
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

    public boolean ensureHeartBreak(Game game) {
        // Assumes that playing the card was legal!
        if (game.getHeartsBroken()) {
            return false; // Already broken, nothing to do
        }

        List<String> currentTrick = game.getCurrentTrick();
        if (currentTrick == null || currentTrick.isEmpty()) {
            return false; // No cards yet
        }

        String lastCardCode = currentTrick.get(currentTrick.size() - 1);
        Card lastCard = CardUtils.fromCode(lastCardCode);

        if ("H".equals(lastCard.getSuit())) {
            game.setHeartsBroken(true);
            return true;
        }
        return false;
    }

    public boolean isGameReadyForResults(Game game) {
        Match match = game.getMatch();
        if (match == null) {
            throw new IllegalStateException("Game has no associated match.");
        }

        int totalCardsRemaining = 0;
        for (MatchPlayer player : match.getMatchPlayers()) {
            String hand = player.getHand();
            if (hand != null && !hand.isEmpty()) {
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
        switch (gameNumber % GameConstants.MAX_TRICK_SIZE) {
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
