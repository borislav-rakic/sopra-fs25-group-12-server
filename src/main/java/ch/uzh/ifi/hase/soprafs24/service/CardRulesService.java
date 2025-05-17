package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // ### FACTS ESTABLISHED

        String playableCards = "";
        // Here goes...
        // Case 0. There are no cards in the player's hand
        if (handSize == 0) {
            // log.info(" ¦ Case 0.");
            log.warn("Player in MatchPlayerSlot={} (MatchPlayerId={}) has no cards in his hands: `{}`.",
                    matchPlayer.getMatchPlayerSlot(), matchPlayer.getMatchPlayerId(),
                    hand);
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
                "   ¦ Playable cards (hand: [{}]) card #{} in trick #{}; cards in trick: [{}] ({}) and hearts are {}broken; verdict: [{}].",
                hand,
                trickSize + 1,
                trickNumber,
                trick,
                leadingSuit.isEmpty() ? "no leading suit" : "leading suit:" + leadingSuit,
                heartsBroken ? "" : "not ",
                playableCards);
        return CardUtils.normalizeCardCodeString(playableCards);

    }

    /**
     * Validates whether the given card code can be legally played by the specified
     * player in the current game state.
     * The method checks the card's format, ensures it is among the playable cards
     * for the player,
     * and confirms the player actually holds the card in their hand.
     *
     * If any check fails, a {@link ResponseStatusException} with HTTP 400 (Bad
     * Request) is thrown.
     *
     * @param game        the current game instance
     * @param matchPlayer the player attempting to play a card
     * @param cardCode    the card code being played (e.g., "QS", "7H")
     * @throws ResponseStatusException if the card is invalid, unplayable, or not
     *                                 held by the player
     */
    public void validateMatchPlayerCardCode(Game game, MatchPlayer matchPlayer, String cardCode) {
        log.info("+++ VALIDATEPLAYERCARDCODE: Trick #{} +++", game.getCurrentTrickNumber());
        log.info("+ Game ID: {}, Player: {}, Attempting to play: {}, Hand: {}",
                game.getGameId(), matchPlayer.getInfo(), cardCode, matchPlayer.getHand());

        // 1. Format check
        if (!CardUtils.isValidCardFormat(cardCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid card format: " + cardCode);
        }

        // 2. Determine playability
        String playableCards = getPlayableCardsForMatchPlayerPlaying(game, matchPlayer);
        boolean cardIsPlayable = playableCards.contains(cardCode);

        // 3. Check if card is playable
        if (!cardIsPlayable) {
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

    /**
     * Rotates the given list so that the element at the specified index becomes the
     * first element.
     * The relative order of the remaining elements is preserved.
     *
     * @param list  the list to rotate
     * @param index the index of the element that should become the first element
     * @param <T>   the type of elements in the list
     * @return a new list with elements rotated to start from the specified index
     */
    private <T> List<T> rotate(List<T> list, int index) {
        List<T> result = new ArrayList<>();
        result.addAll(list.subList(index, list.size()));
        result.addAll(list.subList(0, index));
        return result;
    }

    /**
     * Extracts the suit character from the given card code.
     * The suit is expected to be the last character of the card string (e.g., 'H'
     * for Hearts).
     *
     * @param cardCode the card code to extract the suit from (e.g., "7H", "QS")
     * @return the suit character (e.g., 'H', 'S', 'D', or 'C')
     */
    private char getSuit(String cardCode) {
        return cardCode.charAt(cardCode.length() - 1);
    }

    /**
     * Extracts the numeric value of a card from its card code.
     * Supports face cards ("J", "Q", "K", "A") and handles the special case of "0"
     * representing 10,
     * as used in some external APIs (e.g., "0C" for 10 of Clubs).
     *
     * @param cardCode the card code to extract the value from (e.g., "7H", "QC",
     *                 "0D")
     * @return the numeric value of the card (2–14)
     * @throws IllegalArgumentException if the card code is invalid or the value
     *                                  cannot be parsed
     */
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
     * Calculates the total number of points in the current trick and assigns them
     * to the winning player.
     * Hearts are worth 1 point each, and the Queen of Spades is worth 13 points.
     * Also updates game statistics to track which player received points for each
     * card.
     *
     * @param game                  the game containing the current trick
     * @param winnerMatchPlayerSlot the slot of the player who won the trick (1–4)
     * @return the total number of points earned in the trick
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

    /**
     * Determines the passing direction for a given game round based on the game
     * number.
     * The direction cycles every 4 rounds as follows:
     * - Round 1: Pass to the left
     * - Round 2: Pass across
     * - Round 3: Pass to the right
     * - Round 4: No passing
     *
     * The method returns a map indicating which match player slot passes cards to
     * whom.
     *
     * @param gameNumber the number of the current game (starting from 1)
     * @return a map from source match player slot to target match player slot;
     *         empty if no passing is required
     */
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
        log.info("the passMapp is that of case {}.", gameNumber % GameConstants.MAX_TRICK_SIZE);
        return passMap;
    }

    /**
     * Ensures that Hearts are considered "broken" in the game once a Heart has been
     * played.
     * If the last card played in the current trick is a Heart and Hearts were not
     * already broken,
     * the game's state is updated accordingly.
     *
     * @param game the current game instance
     * @return true if Hearts were just broken by this method; false if they were
     *         already broken or no Heart was played
     */
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

    /**
     * Determines whether the game is ready to transition to the result phase.
     * A game is considered ready for results when all players have no cards left in
     * hand.
     *
     * @param game the game to check
     * @return true if all players have played all their cards; false otherwise
     * @throws IllegalStateException if the game is not associated with a match
     */
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

    /**
     * Checks whether a given card code is present in the list of playable cards.
     *
     * @param cardCode         the card code to check (e.g., "7H", "QS")
     * @param playableCardsCsv a comma-separated string of playable card codes
     *                         (e.g., "2C,QS,10H")
     * @return true if the card code is included in the playable cards list; false
     *         otherwise
     */
    public static boolean isCardCodePlayable(String cardCode, String playableCardsCsv) {
        return Arrays.asList(playableCardsCsv.split(",")).contains(cardCode);
    }

    /**
     * Returns the slot number of the player who will receive cards
     * from the specified MatchPlayer slot during the given game number.
     *
     * @param gameNumber          The current game round
     * @param fromMatchPlayerSlot The slot number of the player passing cards
     * @return The slot number of the player receiving the passed cards
     * @throws IllegalArgumentException if the fromMatchPlayerSlot is invalid
     */
    public Integer getPassingToMatchPlayerSlot(int gameNumber, int fromMatchPlayerSlot) {
        Map<Integer, Integer> passMap = determinePassingDirection(gameNumber);
        Integer toSlot = passMap.get(fromMatchPlayerSlot);
        if (toSlot == null) {
            return fromMatchPlayerSlot;
        }
        return toSlot;
    }

    /**
     * Provides a textual description of the passing direction for the given game
     * round
     * and player slot. The direction cycles based on the game number:
     * - Game 1: pass to the left
     * - Game 2: pass across
     * - Game 3: pass to the right
     * - Game 4: no passing
     *
     * If no passing is required this round, the message indicates so.
     *
     * @param gameNumber          the number of the current game (starting from 1)
     * @param fromMatchPlayerSlot the slot of the player who is passing cards (1–4)
     * @return a descriptive string indicating the target player and passing
     *         direction
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

    /**
     * Returns a message like "Your cards will be passed to X." or
     * "No passing this round." for a particular player in a particular slot
     * 
     * @param match               Relevant Match object.
     * @param gameNumber          Number of current game in this match.
     * @param fromMatchPlayerSlot MatchPlayerSlot of MatchPlayer for whom the
     *                            information is determined.
     **/
    public String namePassingRecipient(Match match, int gameNumber, int fromMatchPlayerSlot) {
        Map<Integer, Integer> passMap = determinePassingDirection(gameNumber);
        Integer toMatchPlayerSlot = passMap.get(fromMatchPlayerSlot);

        if (toMatchPlayerSlot == null) {
            return String.format("No passing this round (Game #%s).", gameNumber);
        }
        log.info("toMatchPlayerSlot is {}.", toMatchPlayerSlot);
        return String.format("Your cards will be passed to %s (Game #%s).",
                match.getNameForMatchPlayerSlot(toMatchPlayerSlot),
                gameNumber);
    }

    /**
     * Checks whether all cards in the given trick are Hearts.
     * Returns false if the trick is empty, null, or contains any non-Heart cards.
     *
     * @param currentTrick the list of card codes representing the current trick
     * @return true if all cards are valid and have the Heart suit ('H'); false
     *         otherwise
     */
    public boolean trickConsistsOnlyOfHearts(List<String> currentTrick) {
        if (currentTrick == null || currentTrick.isEmpty()) {
            return false;
        }

        for (String card : currentTrick) {
            if (card == null || card.length() < 2) {
                return false; // invalid card format
            }

            char suit = card.charAt(card.length() - 1); // suit is the last char
            if (suit != 'H') {
                return false; // not a heart
            }
        }

        return true;
    }

}
