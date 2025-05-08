package ch.uzh.ifi.hase.soprafs24.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.persistence.EntityNotFoundException;

import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
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
import ch.uzh.ifi.hase.soprafs24.entity.MatchSummary;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
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

    @Transactional
    public void advanceTrickPhaseIfOwnerPolling(Game game) {
        if (game.getTrickPhase() == TrickPhase.TRICKJUSTCOMPLETED
                && game.getTrickJustCompletedTime() != null
                && Duration.between(game.getTrickJustCompletedTime(), Instant.now())
                        .toMillis() > GameConstants.TRICK_DELAY_MS) {

            game.setTrickPhase(TrickPhase.PROCESSINGTRICK);
            gameRepository.save(game);
            log.info("Trick marked READY for clearing on next poll.");
            return;
        }
        if (game.getTrickPhase() == TrickPhase.PROCESSINGTRICK) {
            gameTrickService.clearTrick(game.getMatch(), game);
            gameTrickService.updateGamePhaseBasedOnPlayOrder(game);

            // NEW: Detect and set final game phase
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

    @Transactional
    public void finalizeGameScores(Game game) {
        Match match = game.getMatch();
        List<MatchPlayer> players = matchPlayerRepository.findByMatch(match);
        int totalGameScore = players.stream()
                .mapToInt(MatchPlayer::getGameScore)
                .sum();

        log.info("MatchPlayers at finalize: {}",
                match.getMatchPlayers().stream()
                        .map(mp -> mp.getUser().getUsername() + ": " + mp.getGameScore())
                        .toList());

        if (totalGameScore != 26 && totalGameScore != 78) {
            log.warn("Unexpected total score at game end: {}", totalGameScore);
            throw new GameplayException("Scoring error: the total points collected this round are inconsistent.");
        }

        // Handle moon shot
        boolean moonShot = false;
        for (MatchPlayer mp : match.getMatchPlayers()) {
            if (mp.getGameScore() == 26) {
                moonShot = true;
                mp.setGameScore(0);
                mp.setShotTheMoonCount(mp.getShotTheMoonCount() + 1);
            }
        }

        if (moonShot) {
            for (MatchPlayer mp : match.getMatchPlayers()) {
                if (mp.getGameScore() == 0) {
                    mp.setGameScore(26);
                }
            }
        }

        for (MatchPlayer mp : match.getMatchPlayers()) {
            mp.setMatchScore(mp.getMatchScore() + mp.getGameScore());
            mp.setGameScore(0);
            if (mp.getUser() != null && !mp.getIsAiPlayer()) {
                mp.setReady(false);
            }
            matchPlayerRepository.save(mp);
        }

        matchRepository.save(match);
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

        }
        gameRepository.saveAndFlush(game);
    }

    public Game getActiveGameByMatchId(Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new EntityNotFoundException("Match not found");
        }

        // Always fetch the Game from the database based on match and phase
        Game game = gameRepository.findActiveGameByMatchId(match.getMatchId());

        if (game == null) {
            throw new IllegalStateException("No active game found for this match");
        }

        return game;
    }

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

    public void setExistingGameSummaryOrCreateIt(Match match, String gameSummaryHtml) {
        MatchSummary matchSummary = match.getMatchSummary();

        if (matchSummary == null) {
            matchSummary = new MatchSummary();
            match.setMatchSummary(matchSummary); // Ensure bidirectional consistency
        }

        if (gameSummaryHtml != null) {
            matchSummary.setGameSummaryHtml(gameSummaryHtml);
        }

        // Persist the match (and MatchSummary if cascade is enabled)
        matchRepository.save(match);
    }

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

    public int findTwoOfClubsLeaderSlot(Game game) {
        Match match = game.getMatch();
        for (MatchPlayer player : match.getMatchPlayers()) {
            if (player.hasCardCodeInHand(GameConstants.TWO_OF_CLUBS)) {
                int slot = player.getMatchPlayerSlot();
                log.info("° 2C assigned to matchPlayerSlot {}.", slot);
                return slot;
            }
        }

        throw new IllegalStateException("No player has the 2♣ — invalid game state.");
    }

    @Transactional
    public void fastForwardGameInternally(Game game) {
        Match match = game.getMatch();

        if (game.getTrickPhase() == TrickPhase.TRICKJUSTCOMPLETED) {
            // Skip wait time — treat as instantly complete
            game.setTrickPhase(TrickPhase.PROCESSINGTRICK);
            log.info("Fast-forward: skipping trick pause.");
        }

        if (game.getTrickPhase() == TrickPhase.PROCESSINGTRICK) {
            gameTrickService.clearTrick(match, game);
            gameTrickService.updateGamePhaseBasedOnPlayOrder(game);

            if (finalizeGameIfComplete(game)) {
                log.info("Fast-forward: game completed.");
                return;
            }

            game.setTrickPhase(TrickPhase.READYFORFIRSTCARD);
            gameRepository.save(game);
            log.info("Fast-forward: trick cleared and next one ready.");
        }
    }

}
