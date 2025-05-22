package ch.uzh.ifi.hase.soprafs24.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.AiMatchPlayerState;
import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchMessageType;
import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

/**
 * Game Service
 * This class is the "worker" and responsible for all functionality related to
 * currently ongoing games, e.g. updating the player's scores, requesting
 * information
 * from the deck of cards API, etc.
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */

@Service
@Transactional
public class GameService {
    private final AiPlayingService aiPlayingService;
    private final CardPassingService cardPassingService;
    private final CardRulesService cardRulesService;
    private final GameRepository gameRepository;
    private final GameStatsService gameStatsService;
    private final GameTrickService gameTrickService;
    private final MatchRepository matchRepository;
    private final MatchMessageService matchMessageService;
    private final MatchPlayerRepository matchPlayerRepository;
    private final MatchSummaryService matchSummaryService;

    // or via constructor injection

    @Autowired
    public GameService(
            @Qualifier("aiPlayingService") AiPlayingService aiPlayingService,
            @Qualifier("cardPassingService") CardPassingService cardPassingService,
            @Qualifier("cardRulesService") CardRulesService cardRulesService,
            @Qualifier("gameRepository") GameRepository gameRepository,
            @Qualifier("gameStatsService") GameStatsService gameStatsService,
            @Qualifier("gameTrickService") GameTrickService gameTrickService,
            @Qualifier("matchMessageService") MatchMessageService matchMessageService,
            @Qualifier("matchRepository") MatchRepository matchRepository,
            @Qualifier("matchPlayerRepository") MatchPlayerRepository matchPlayerRepository,
            @Qualifier("matchSummaryService") MatchSummaryService matchSummaryService) {
        this.aiPlayingService = aiPlayingService;
        this.cardPassingService = cardPassingService;
        this.cardRulesService = cardRulesService;
        this.gameRepository = gameRepository;
        this.gameStatsService = gameStatsService;
        this.gameTrickService = gameTrickService;
        this.matchMessageService = matchMessageService;
        this.matchRepository = matchRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchSummaryService = matchSummaryService;

    }

    private static final Logger log = LoggerFactory.getLogger(GameService.class);

    /**
     * Executes a single AI player's turn in the given match and game.
     * The AI will first "think" for one turn (delaying action), then on the next
     * call,
     * select and play a card using a predefined strategy.
     *
     * Validates that:
     * - the provided player is an AI,
     * - it is currently their turn,
     * - the game has not already completed all plays.
     *
     * @param match    the match in which the game is being played
     * @param game     the current game instance
     * @param aiPlayer the AI-controlled player attempting to take a turn
     * @return true if the AI successfully played a card, false if they are still
     *         "thinking" or the game is over
     * @throws ResponseStatusException if:
     *                                 - the given player is not a valid AI player
     *                                 for the match ({@code CONFLICT}),
     *                                 - it is not the AI player's turn to act
     *                                 ({@code CONFLICT})
     */
    public boolean playSingleAiTurn(Match match, Game game, MatchPlayer aiPlayer) {
        // Is this aiPlayer an existing AI Player?
        if (aiPlayer == null || !Boolean.TRUE.equals(aiPlayer.getIsAiPlayer())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The MatchPlayer is not an AI Player of this match.");
        }
        // Is it really this players turn?
        if (game.getCurrentMatchPlayerSlot() != aiPlayer.getMatchPlayerSlot()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "It is not this AI player's turn.");
        }
        // Have all cards already been played in this game?
        if (game.getCurrentPlayOrder() > GameConstants.FULL_DECK_CARD_COUNT) {
            return false;
        }

        // Start "thinking".
        if (aiPlayer.getAiMatchPlayerState() == AiMatchPlayerState.READY) {
            aiPlayer.setAiMatchPlayerState(AiMatchPlayerState.THINKING);
            matchPlayerRepository.save(aiPlayer);
            log.info("  = The AI Player in Slot {} skips a turn to think.", aiPlayer.getMatchPlayerSlot());
            return false;
        }
        // Do the actual "thinking".
        aiPlayer.setAiMatchPlayerState(AiMatchPlayerState.READY);
        log.info("  = The AI Player in Slot {} is done thinking and ready to perform their turn.",
                aiPlayer.getMatchPlayerSlot());

        // Select a card to play.
        String cardCode = aiPlayingService.selectCardToPlay(game, aiPlayer, Strategy.LEFTMOST);

        // Play that card.
        playCardAsAi(game, aiPlayer, cardCode);

        return true;
    }

    /**
     * Handles a human player's attempt to play a card during their turn in the
     * game.
     * Performs all necessary validations to ensure the game is active, it's the
     * player's turn,
     * and the card being played is legal according to game rules. If the player
     * submits a placeholder
     * card code ("XX"), a card is selected automatically using a random AI
     * strategy.
     *
     * This method is transactional to ensure consistency during state transitions
     * and updates.
     *
     * @param game        the current game instance
     * @param matchPlayer the player attempting to play a card
     * @param cardCode    the code of the card the player is attempting to play
     *                    (e.g., "7H", "QS", or "XX" for random)
     * @throws ResponseStatusException if the game is not in an active phase
     *                                 ({@code FORBIDDEN})
     * @throws GameplayException       if the player attempts to act out of turn or
     *                                 play an invalid card
     */
    @Transactional
    public void playCardAsHuman(Game game, MatchPlayer matchPlayer, String cardCode) {
        log.info("=== PLAY CARD AS HUMAN (playOrder={}), CurrentSlot: {}.",
                game.getCurrentPlayOrder(), game.getCurrentMatchPlayerSlot());
        log.info("  = HUMAN at matchPlayerSlot {} attempting to play card {}.", matchPlayer.getInfo(),
                cardCode);
        // Defensive checks
        if (game.getPhase().isNotActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Game is not active.");
        }
        assertConsistentGameState(game);
        int matchPlayerSlot = matchPlayer.getMatchPlayerSlot();
        int playerSlot = matchPlayerSlot - 1;
        int currentMatchPlayerSlot = game.getCurrentMatchPlayerSlot();
        int currentPlayerSlot = currentMatchPlayerSlot - 1;
        if (matchPlayerSlot != game.getCurrentMatchPlayerSlot()) {
            throw new GameplayException(
                    String.format("Not your turn, playerSlot %d; currentPlayerSlot is %d.", playerSlot,
                            currentPlayerSlot));
        }
        log.info("= HUMAN at matchPlayerSlot {} attempting to play card {}.", matchPlayer.getInfo(),
                cardCode);
        log.info(
                "= Cards in hand are: {}. TrickLeader is: {}. Cards played in this trick: {}. PlayOrder: {}. TrickNumber: {}",
                matchPlayer.getHand(),
                game.getTrickLeaderMatchPlayerSlot(), game.getCurrentTrick(), game.getCurrentPlayOrder(),
                game.getCurrentTrickNumber());

        if ("XX".equals(cardCode)) {
            cardCode = aiPlayingService.selectCardToPlay(game, matchPlayer, Strategy.RANDOM);
        }
        cardRulesService.validateMatchPlayerCardCode(game, matchPlayer, cardCode);
        log.info(
                "= About to executeValidatedCardPlay");
        executeValidatedCardPlay(game, matchPlayer, cardCode);
        log.info("=== PLAY CARD AS HUMAN CONCLUDED ({}) ===", game.getCurrentPlayOrder());

    }

    /**
     * Executes a card play for an AI-controlled player.
     * Validates that the card format is correct, the card is in the AI player's
     * hand,
     * the AI player is valid and it's their turn, and the play is legal according
     * to game rules.
     * Then plays the card by invoking the validated play execution.
     *
     * This method is transactional to ensure atomic updates to the game and player
     * state.
     *
     * @param game     the game in which the AI is playing
     * @param aiPlayer the AI-controlled {@link MatchPlayer} making the move
     * @param cardCode the card code to be played (e.g., "QS", "10H")
     *
     * @throws ResponseStatusException if:
     *                                 - the AI player has no cards,
     *                                 - the card is not in the AI player’s hand
     *                                 ({@code BAD_REQUEST})
     * @throws IllegalStateException   if:
     *                                 - the player is not marked as an AI,
     *                                 - the slot is invalid,
     *                                 - the AI attempts to play out of turn
     */
    @Transactional
    public void playCardAsAi(Game game, MatchPlayer aiPlayer, String cardCode) {
        CardUtils.requireValidCardFormat(cardCode);

        String hand = aiPlayer.getHand();
        if (hand == null || hand.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The AiPlayer {} has no cards in hand.", aiPlayer.getInfo()));
        }

        if (!aiPlayer.hasCardCodeInHand(cardCode)) {
            int playerSlot = aiPlayer.getMatchPlayerSlot();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The AiPlayer in playerSlot " + playerSlot + " does not have the card " + cardCode
                            + " in hand.");
        }

        int matchPlayerSlot = aiPlayer.getMatchPlayerSlot();
        if (matchPlayerSlot < 1 || matchPlayerSlot > GameConstants.MAX_TRICK_SIZE) {
            int playerSlot = matchPlayerSlot - 1;
            throw new IllegalStateException(
                    "The AI player " + aiPlayer.getMatchPlayerId() + " is in an invalid playerSlot (" + playerSlot
                            + ").");
        }

        if (!Boolean.TRUE.equals(aiPlayer.getUser().getIsAiPlayer())) {
            int playerSlot = matchPlayerSlot - 1;
            throw new IllegalStateException("playerSlot " + playerSlot + " is not controlled by an AI player.");
        }

        if (matchPlayerSlot != game.getCurrentMatchPlayerSlot()) {
            int playerSlot = matchPlayerSlot - 1;
            throw new IllegalStateException("AI tried to play out of turn (playerSlot " + playerSlot + ").");
        }

        cardRulesService.validateMatchPlayerCardCode(game, aiPlayer, cardCode);
        executeValidatedCardPlay(game, aiPlayer, cardCode);
    }

    /**
     * Executes a validated card play for the given player and updates all related
     * game state.
     * Removes the card from the player's hand, adds it to the current trick,
     * updates the game phase,
     * adds contextual match messages (e.g., for Queen of Spades or hearts broken),
     * and triggers trick and stats updates.
     * Assumes the card has already been validated for playability.
     *
     * This method is transactional to ensure consistency across multiple related
     * updates.
     *
     * @param game        the current game instance in which the card is being
     *                    played
     * @param matchPlayer the player playing the card
     * @param cardCode    the code of the card being played (e.g., "QS", "10H")
     *
     * @throws IllegalStateException if the card could not be removed from the
     *                               player's hand (e.g., not present)
     */
    @Transactional
    public void executeValidatedCardPlay(Game game, MatchPlayer matchPlayer, String cardCode) {
        log.info("   +-- executeValidatedCardPlay ---");

        if (!matchPlayer.removeCardCodeFromHand(cardCode)) {
            throw new IllegalStateException("Tried to remove a card that wasn't in hand: " + cardCode);
        }

        log.info("    + executeValidatedCardPlay just about to addCardToTrick({}). GamePhase={}.", cardCode,
                game.getPhase());
        gameTrickService.addCardToTrick(game.getMatch(), game, matchPlayer, cardCode);
        log.info("    + executeValidatedCardPlay just after addCardToTrick({}). GamePhase={}.", cardCode,
                game.getPhase());
        gameTrickService.updateGamePhaseBasedOnPlayOrder(game);

        log.info("    + executeValidatedCardPlay just after updateGamePhaseBasedOnPlayOrder({}). GamePhase={}.",
                cardCode,
                game.getPhase());
        if (GameConstants.QUEEN_OF_SPADES.equals(cardCode)) {
            matchMessageService.addMessage(
                    game.getMatch(),
                    MatchMessageType.QUEEN_WARNING,
                    matchMessageService.getFunMessage(MatchMessageType.QUEEN_WARNING));
        }

        if (cardRulesService.ensureHeartBreak(game)) {
            matchMessageService.addMessage(
                    game.getMatch(),
                    MatchMessageType.HEARTS_BROKEN,
                    matchMessageService.getFunMessage(MatchMessageType.HEARTS_BROKEN));
        }

        if (game.getCurrentPlayOrder() == 1) {
            matchMessageService.addMessage(
                    game.getMatch(),
                    MatchMessageType.GAME_STARTED,
                    matchMessageService.getFunMessage(MatchMessageType.GAME_STARTED));
        }

        gameStatsService.updateGameStatsAfterTrickChange(game);

        gameTrickService.afterCardPlayed(game); // advancing state + checking trick completion

        gameStatsService.recordCardPlay(game, matchPlayer, cardCode);

        log.info("   +--- executeValidatedCardPlay ---");
    }

    /**
     * Advances the trick phase of the game if the polling player is the match owner
     * and enough time has passed
     * since the last trick was completed. This method ensures smooth transition
     * between trick phases
     * by:
     * - Moving from {@code TRICKJUSTCOMPLETED} to {@code PROCESSINGTRICK} after a
     * delay
     * - Clearing the trick and updating game state during {@code PROCESSINGTRICK}
     * - Transitioning to {@code RESULT} phase if all cards have been played
     * Posting appropriate match messages based on trick contents (e.g., all
     * hearts, last trick)
     *
     * This method is typically triggered by polling logic and is only
     * executed by the match host to avoid duplicate transitions.
     *
     * @param game the current {@link Game} instance to check and update
     */
    @Transactional
    public void advanceTrickPhaseIfOwnerPolling(Game game) {
        if (game.getTrickPhase() == TrickPhase.TRICKJUSTCOMPLETED
                && game.getTrickJustCompletedTime() != null
                && Duration.between(game.getTrickJustCompletedTime(), Instant.now())
                        .toMillis() > GameConstants.TRICK_DELAY_MS) {

            game.setTrickPhase(TrickPhase.PROCESSINGTRICK);
            gameRepository.save(game);
            log.info("Trick marked READY for clearing on next poll.");
            if (cardRulesService.trickConsistsOnlyOfHearts(game.getCurrentTrick())
                    && game.getPhase() != GamePhase.FINALTRICK) {
                matchMessageService.addMessage(
                        game.getMatch(),
                        MatchMessageType.ALL_HEARTS_TRICK,
                        matchMessageService.getFunMessage(MatchMessageType.ALL_HEARTS_TRICK));
            }
            return;
        }
        if (game.getTrickPhase() == TrickPhase.PROCESSINGTRICK) {

            gameTrickService.clearTrick(game.getMatch(), game);
            gameTrickService.updateGamePhaseBasedOnPlayOrder(game);

            if (game.getCurrentPlayOrder() == 48) {
                matchMessageService.addMessage(
                        game.getMatch(),
                        MatchMessageType.LAST_TRICK_STARTED,
                        matchMessageService.getFunMessage(MatchMessageType.LAST_TRICK_STARTED));
            }

            // Detect and set final game phase
            if (game.getCurrentPlayOrder() == GameConstants.FULL_DECK_CARD_COUNT &&
                    game.getPhase() != GamePhase.RESULT) {

                log.info("All cards played — setting GamePhase to RESULT.");
                game.setPhase(GamePhase.RESULT);
            }

            // Proceed with finalization
            if (finalizeGameIfComplete(game)) {
                return; // Game is done
            }

            gameRepository.save(game);

            log.info("Trick cleared and new one started. Trick #: {}, Leader: {}",
                    game.getCurrentTrickNumber(),
                    game.getTrickLeaderMatchPlayerSlot());
        }
    }

    /**
     * Finalizes the game if it has reached the RESULT phase and all cards have been
     * played.
     * 
     * @param game the game to check and finalize
     * @return true if the game was finalized, false if it was not yet complete
     */
    @Transactional
    public boolean finalizeGameIfComplete(Game game) {
        if (game.getPhase() != GamePhase.RESULT) {
            return false;
        }

        // Defensive: Only run once
        if (game.getCurrentPlayOrder() != GameConstants.FULL_DECK_CARD_COUNT) {
            return false;
        }

        // Optional: you could also check if match scores already include the game's
        // points

        finalizeGameScores(game);
        return true;
    }

    /**
     * Finalizes the scores for the given game by applying game results to each
     * player,
     * handling moon shots and perfect games, updating match scores, and saving
     * game and match state.
     *
     * @param game the completed game to finalize scores for
     */
    @Transactional
    public void finalizeGameScores(Game game) {
        Match match = game.getMatch();
        String newsFlash = "";
        List<MatchPlayer> players = matchPlayerRepository.findByMatch(match);
        int totalGameScore = players.stream()
                .mapToInt(MatchPlayer::getGameScore)
                .sum();

        log.info("MatchPlayers at finalize: {}",
                match.getMatchPlayersSortedBySlot().stream()
                        .map(mp -> mp.getUser().getUsername() + ": " + mp.getGameScore())
                        .toList());

        if (totalGameScore != 26 && totalGameScore != 78) {
            log.warn("Unexpected total score at game end: {}", totalGameScore);
        }

        // Handle moon shot
        boolean moonShot = false;
        for (MatchPlayer mp : match.getMatchPlayers()) {
            if (mp.getGameScore() == 26) {
                moonShot = true;
                mp.setGameScore(-1);
                mp.setShotTheMoonCount(mp.getShotTheMoonCount() + 1);
                newsFlash += String.format("<div class=\"modalMessageNewsFlashItem\">Congrats: %s shot the Moon!</div>",
                        mp.getUser().getUsername());
            }
        }

        // Handle perfect games (only if nobody shot the moon, though)
        if (!moonShot) {
            for (MatchPlayer mp : match.getMatchPlayers()) {
                if (mp.getGameScore() == 0) {
                    mp.setPerfectGames(mp.getPerfectGames() + 1);
                    newsFlash += String.format("<div class=\"modalMessageNewsFlashItem\">A perfect game for %s!</div>",
                            mp.getUser().getUsername());
                }
            }
        }

        if (moonShot) {
            for (MatchPlayer mp : match.getMatchPlayers()) {
                if (mp.getGameScore() == 0) {
                    mp.setGameScore(26);
                } else if (mp.getGameScore() == -1) {
                    mp.setGameScore(0);
                }
            }
        }

        // Finalize and validate game scores
        List<MatchPlayer> sortedPlayers = match.getMatchPlayersSortedBySlot();

        // Step 1: Capture game scores before they get reset
        List<Integer> gameScores = sortedPlayers.stream()
                .map(MatchPlayer::getGameScore)
                .collect(Collectors.toList());
        game.setGameScoresList(gameScores);
        gameRepository.save(game);

        // Step 2: Update player match scores and reset game scores
        for (MatchPlayer mp : sortedPlayers) {
            mp.setMatchScore(mp.getMatchScore() + mp.getGameScore());
            mp.setGameScore(0);
            if (mp.getUser() != null && !mp.getIsAiPlayer()) {
                mp.setReady(false);
            }
            matchPlayerRepository.save(mp);
        }

        // Step 3: Write updated names and scores (ordered by slot)
        List<String> playerNames = sortedPlayers.stream()
                .map(mp -> mp.getUser() != null ? mp.getUser().getUsername() : "AI")
                .collect(Collectors.toList());
        List<Integer> matchScores = sortedPlayers.stream()
                .map(MatchPlayer::getMatchScore)
                .collect(Collectors.toList());

        match.setMatchPlayerNames(playerNames);
        match.setMatchScoresList(matchScores);

        // Save summary and match
        log.info("finalizeGameScores calling buildGameResultHtml");
        matchSummaryService.saveGameResultHtml(match, game, newsFlash);

        matchRepository.save(match);

    }

    /**
     * Given a a match, deletes all gamestats for that match.
     * 
     * @param match The relevant Match object.
     */
    public void clearAllMatchStatsForGame(Match match) {
        gameStatsService.deleteGameStatsForMatch(match);
        matchRepository.save(match);
    }

    public void maybeTriggerAiCardPassing(Game game) {
        if (game == null) {
            log.warn("maybeTriggerAiCardPassing: game is null");
            return;
        }

        if (game.getPhase() != GamePhase.PASSING) {
            log.info("maybeTriggerAiCardPassing: Skipping because game is not in PASSING phase.");
            return;
        }

        cardPassingService.maybeTriggerAiPassing(game);
    }

    @Transactional
    public void resetNonAiPlayersReady(Game game) {
        Match match = game.getMatch();

        // Iterate through each match player in the match
        for (MatchPlayer matchPlayer : match.getMatchPlayers()) {
            // Only reset readiness for non-AI players
            if (!Boolean.TRUE.equals(matchPlayer.getIsAiPlayer())) {
                matchPlayer.setReady(false);
                log.info("    🫡 MatchPlayer id={} in slot={} was set to ready=false.",
                        matchPlayer.getUser().getId(),
                        matchPlayer.getMatchPlayerSlot());
            }
        }

        // Save the match if changes were made (e.g., readiness reset)
        matchRepository.save(match); // Ensure changes are persisted
    }

    /**
     * Relays a message to the {@link MatchMessageService}, optionally including a
     * player's name.
     * If {@code who} is provided and not blank, the message is personalized using
     * the player's name.
     * Otherwise, a generic message is added.
     *
     * @param match            the match in which the message should be recorded
     * @param matchMessageType the type of message to be added
     * @param who              the name of the player involved in the message (can
     *                         be null or blank for generic messages)
     */
    public void relayMessageToMatchMessageService(
            Match match,
            MatchMessageType matchMessageType,
            String who) {

        if (who == null || who.isBlank()) {
            matchMessageService.addMessage(match, matchMessageType,
                    matchMessageService.getFunMessage(matchMessageType));
        } else {
            matchMessageService.addMessage(match, matchMessageType,
                    matchMessageService.getFunMessage(matchMessageType, who));
        }
    }

    /**
     * Accepts and processes a set of cards passed by a player during the passing
     * phase of the game.
     * Delegates the handling of card validation and saving to the
     * {@code cardPassingService}.
     * If all 12 cards (3 from each of 4 players) have been passed, the method:
     * - Collects and reassigns the passed cards to their new owners
     * - Sets the player who holds the Two of Clubs as the starting leader
     * - Transitions the game into the {@code FIRSTTRICK} phase
     *
     * @param game         the current game instance
     * @param matchPlayer  the player submitting passed cards
     * @param passingDTO   the DTO containing the cards to be passed
     * @param pickRandomly whether the cards should be chosen randomly (e.g. by AI)
     *
     * @throws ResponseStatusException if invalid card selections are made (handled
     *                                 internally)
     */
    @Transactional
    public void passingAcceptCards(Game game, MatchPlayer matchPlayer, GamePassingDTO passingDTO,
            Boolean pickRandomly) {
        int passedCount = cardPassingService.passingAcceptCards(game, matchPlayer, passingDTO, pickRandomly);

        // If all 12 cards passed, proceed to collect
        if (passedCount == 12) {
            cardPassingService.collectPassedCards(game);
            log.info("°°° PASSING CONCLUDED °°°");
            assignTwoOfClubsLeader(game);
            // Transition phase to FIRSTTRICK!
            game.setCurrentTrickNumber(1);
            game.setCurrentPlayOrder(0);
            game.setPhase(GamePhase.FIRSTTRICK);
            game.setTrickPhase(TrickPhase.READYFORFIRSTCARD);
            gameRepository.save(game);
            log.info("/// READY TO PLAY FIRST TRICK ///");

            log.info("Checking cards distribution AFTER passing.");
            cardRulesService.validateUniqueDeckAcrossPlayers(game.getMatch());

        }
        gameRepository.saveAndFlush(game);
    }

    /**
     * Resets the "ready" status for all players in the specified match.
     * This is typically used to prepare the game state for the next round or phase.
     * Ensures that exactly 4 players are present before performing the reset.
     *
     * @param matchId the ID of the match whose players should be reset
     * @throws IllegalArgumentException if the match is not found
     * @throws IllegalStateException    if the match does not have exactly 4 players
     */
    @Transactional
    public void resetAllPlayersReady(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));

        List<MatchPlayer> players = match.getMatchPlayers();

        if (players.size() != GameConstants.MAX_TRICK_SIZE) {
            throw new IllegalStateException("Expected 4 match players, but found " + players.size());
        }

        for (MatchPlayer player : players) {
            player.resetReady(); // Custom logic
        }
    }

    /**
     * Validates that the current game state is internally consistent.
     * Ensures the game is linked to a match, the game phase aligns with the match
     * phase,
     * the play order matches the current trick number, and trick phase consistency
     * is upheld.
     *
     * @param game the game instance to check
     * @throws IllegalStateException if any state inconsistency is found
     */
    @Transactional
    public void assertConsistentGameState(Game game) {
        Match match = game.getMatch();
        if (match == null) {
            throw new IllegalStateException("Game does not belong to match.");
        }
        // GamePhase vs. MatchPhase
        if (game.getPhase().onGoing() && !match.getPhase().inGame()) {
            throw new IllegalStateException(String.format(
                    "Illegal Game State: game is in phase %s, but match in phase %s.",
                    game.getPhase(),
                    match.getPhase()));
        }
        // PlayOrder vs. TrickNumber
        if (game.getCurrentPlayOrder() >= 0
                && game.getCurrentPlayOrder() / GameConstants.MAX_TRICK_SIZE + 1 != game.getCurrentTrickNumber()) {

            throw new IllegalStateException(String.format(
                    "Illegal Game State: playOrder is %d, but trickNumber is %d.",
                    game.getCurrentPlayOrder(),
                    game.getCurrentTrickNumber()));
        }
        // PlayOrder vs. GamePhase
        doPlayOrderAndTrickPhaseMatch(game);
    }

    /**
     * Checks that the game's current play order is valid and consistent with its
     * phase and trick phase.
     *
     * This method verifies that the play order falls within the expected range for
     * the game's phase:
     * - FIRSTTRICK: play order should be between 0 and 4
     * - NORMALTRICK: play order should be between 4 and 48
     * - FINALTRICK: play order should be between 48 and 52
     * - RESULT: play order should be exactly 52
     * - PASSING: play order should be 0
     *
     * If the trick is in a transition phase, the upper bound is inclusive.
     * Throws an exception if any condition is violated.
     *
     * @param game the game whose play order and phase should be validated
     * @throws IllegalStateException if the play order is inconsistent with the game
     *                               phase
     */
    public void doPlayOrderAndTrickPhaseMatch(Game game) {
        TrickPhase trickPhase = game.getTrickPhase();
        int playOrder = game.getCurrentPlayOrder();
        GamePhase phase = game.getPhase();

        boolean isTrickBetween = trickPhase.inTransition();

        if (phase == GamePhase.FIRSTTRICK) {
            if (playOrder < 0 || (!isTrickBetween && playOrder > GameConstants.MAX_TRICK_SIZE)) {
                throw new IllegalStateException(String.format(
                        "Invalid playOrder %d for phase FIRSTTRICK.", playOrder));
            }
        } else if (phase == GamePhase.NORMALTRICK) {
            if (playOrder < 4 || (!isTrickBetween && playOrder > 48)) {
                throw new IllegalStateException(String.format(
                        "Invalid playOrder %d for phase NORMALTRICK.", playOrder));
            }
        } else if (phase == GamePhase.FINALTRICK) {
            if (playOrder < 48 || playOrder > GameConstants.FULL_DECK_CARD_COUNT) {
                throw new IllegalStateException(String.format(
                        "Invalid playOrder %d for phase FINALTRICK.", playOrder));
            }
        } else if (phase == GamePhase.RESULT) {
            if (playOrder < GameConstants.FULL_DECK_CARD_COUNT) {
                throw new IllegalStateException(String.format(
                        "Invalid playOrder %d for phase RESULT.", playOrder));
            }
        } else if (phase == GamePhase.PASSING) {
            if (playOrder != 0) {
                throw new IllegalStateException("During PASSING phase, playOrder should be 0.");
            }
        }
    }

    /**
     * Assigns the first trick leader based on who holds the Two of Clubs.
     *
     * Searches all players in the match for the one who has the Two of Clubs in
     * hand.
     * Sets that player's slot as the current match player slot and trick leader
     * slot.
     *
     * @param game the game instance to assign the trick leader for
     * @throws IllegalStateException if no player holds the Two of Clubs
     */
    public void assignTwoOfClubsLeader(Game game) {
        Match match = game.getMatch();
        for (MatchPlayer player : match.getMatchPlayers()) {
            if (player.hasCardCodeInHand(GameConstants.TWO_OF_CLUBS)) {
                int slot = player.getMatchPlayerSlot();
                game.setCurrentMatchPlayerSlot(slot);
                game.setTrickLeaderMatchPlayerSlot(slot);

                log.info("° TrickLeaderMatchPlayerSlot was assigned to MatchPlayerSlot{}.",
                        slot);

                return;
            }
        }

        throw new IllegalStateException("No player has the 2♣ — invalid game state.");
    }

}
