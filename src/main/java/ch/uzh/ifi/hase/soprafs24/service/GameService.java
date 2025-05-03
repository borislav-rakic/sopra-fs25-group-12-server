package ch.uzh.ifi.hase.soprafs24.service;

import java.util.List;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchMessageType;
import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
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
    private final MatchRepository matchRepository;
    private final MatchMessageService matchMessageService;

    // or via constructor injection

    @Autowired
    public GameService(
            @Qualifier("aiPlayingService") AiPlayingService aiPlayingService,
            @Qualifier("cardPassingService") CardPassingService cardPassingService,
            @Qualifier("cardRulesService") CardRulesService cardRulesService,
            @Qualifier("gameRepository") GameRepository gameRepository,
            @Qualifier("gameStatsService") GameStatsService gameStatsService,
            @Qualifier("matchMessageService") MatchMessageService matchMessageService,
            @Qualifier("matchRepository") MatchRepository matchRepository) {
        this.aiPlayingService = aiPlayingService;
        this.cardPassingService = cardPassingService;
        this.cardRulesService = cardRulesService;
        this.gameRepository = gameRepository;
        this.gameStatsService = gameStatsService;
        this.matchMessageService = matchMessageService;
        this.matchRepository = matchRepository;

    }

    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private static final Boolean PLAY_ALL_AI_TURNS_AT_ONCE = false;

    public void playAiTurnsUntilHuman(Long gameId) {
        while (true) {
            boolean played = playSingleAiTurn(gameId);
            if (!played || !PLAY_ALL_AI_TURNS_AT_ONCE) {
                break;
            }
        }
    }

    public boolean playSingleAiTurn(Long gameId) {
        Game game = gameRepository.findByGameId(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find game.");
        }

        Match match = game.getMatch();
        MatchPlayer aiPlayer = match.requireMatchPlayerBySlot(game.getCurrentMatchPlayerSlot());

        if (aiPlayer == null || !Boolean.TRUE.equals(aiPlayer.getIsAiPlayer())) {
            if (game.getCurrentPlayOrder() > GameConstants.FULL_DECK_CARD_COUNT) {
                return false;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not an AI turn.");
        }

        String cardCode = aiPlayingService.selectCardToPlay(game, aiPlayer, Strategy.LEFTMOST);

        try {
            Thread.sleep(50); // simulate thinking time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        playCardAsAi(game, aiPlayer, cardCode);

        return true;
    }

    public void playCardAsHuman(Game game, MatchPlayer matchPlayer, String cardCode) {
        log.info("=== PLAY CARD AS HUMAN ({}), CurrentSlot: {}.",
                game.getCurrentPlayOrder(), game.getCurrentMatchPlayerSlot());
        log.info("= HUMAN at matchPlayerSlot {} attempting to play card {}.", matchPlayer.getInfo(),
                cardCode);
        // Defensive checks
        if (game.getPhase().isNotActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Game is not active.");
        }
        int matchPlayerSlot = matchPlayer.getMatchPlayerSlot();
        int playerSlot = matchPlayerSlot - 1;
        int currentMatchPlayerSlot = game.getCurrentMatchPlayerSlot();
        int currentPlayerSlot = currentMatchPlayerSlot - 1;
        if (matchPlayerSlot != game.getCurrentMatchPlayerSlot()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
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

        if (cardCode == "XX") {
            cardCode = aiPlayingService.selectCardToPlay(game, matchPlayer, Strategy.RANDOM);
        }
        cardRulesService.validateMatchPlayerCardCode(game, matchPlayer, cardCode);
        log.info(
                "= About to executeValidatedCardPlay");
        executeValidatedCardPlay(game, matchPlayer, cardCode);
        log.info("=== PLAY CARD AS HUMAN CONCLUDED ({}) ===", game.getCurrentPlayOrder());
    }

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
        if (matchPlayerSlot < 1 || matchPlayerSlot > 4) {
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

    public void executeValidatedCardPlay(Game game, MatchPlayer matchPlayer, String cardCode) {
        log.info("   +-- executeValidatedCardPlay ---");

        log.info("   | Playing card: {} by MatchPlayer {} (before play: hand = {})",
                cardCode, matchPlayer.getInfo(), matchPlayer.getHand());

        // Step 1: Remove the card from hand
        if (!matchPlayer.removeCardCodeFromHand(cardCode)) {
            throw new IllegalStateException("Tried to remove a card that wasn't in hand: " + cardCode);
        }

        // Step 2: Add the card to the trick
        game.addCardCodeToCurrentTrick(cardCode);
        if (cardCode == "QS") {
            matchMessageService.addMessage(
                    game.getMatch(),
                    MatchMessageType.QUEEN_WARNING,
                    matchMessageService.getFunMessage(MatchMessageType.QUEEN_WARNING));
        }
        log.info("   | Card {} added to trick: {}. Hand now: {}", cardCode, game.getCurrentTrick(),
                matchPlayer.getHand());
        game.setCurrentPlayOrder(game.getCurrentPlayOrder() + 1);
        log.info("CURRENTPLAYORDER {}", game.getCurrentPlayOrder());
        game.updateGamePhaseBasedOnPlayOrder();

        // Step 3: Advance the turn if trick is not complete
        if (game.getCurrentTrickSize() < 4) {
            int nextSlot = (game.getCurrentMatchPlayerSlot() % 4) + 1;
            game.setCurrentMatchPlayerSlot(nextSlot);
            log.info("   | Turn advanced to next matchPlayerSlot: {}", game.getCurrentMatchPlayerSlot());
        }

        // Step 4: Handle potential trick completion
        handlePotentialTrickCompletion(game);

        // Step 5: Record the completed play — now that state is stable
        gameStatsService.recordCardPlay(game, matchPlayer, cardCode);
        log.info("   | Stats recorded for card {} by MatchPlayer {}", cardCode, matchPlayer.getInfo());
        log.info("   +--- executeValidatedCardPlay ---");
    }

    private void handlePotentialTrickCompletion(Game game) {
        log.info(" (No trick completion yet.)");
        if (game.getCurrentTrickSize() != 4) {
            return; // Trick is not complete yet
        }
        log.info(" &&& TRICK COMPLETION: {}. &&&", game.getCurrentTrick());

        // Step 1: Determine winner and points
        int winnerMatchPlayerSlot = cardRulesService.determineTrickWinner(game);
        int points = cardRulesService.calculateTrickPoints(game.getCurrentTrick());

        log.info(" & Trick winnerMatchPlayerSlot {} ({} points)", winnerMatchPlayerSlot, points);

        // Step 2: Archive the trick
        game.setPreviousTrick(game.getCurrentTrick());
        game.setPreviousTrickWinnerMatchPlayerSlot(winnerMatchPlayerSlot);
        game.setPreviousTrickPoints(points);
        game.setPreviousTrickLeaderMatchPlayerSlot(game.getTrickLeaderMatchPlayerSlot());

        // Step 3: Prepare for the next trick
        game.updateGamePhaseBasedOnPlayOrder();
        game.setCurrentTrickNumber(game.getCurrentTrickNumber() + 1);
        game.clearCurrentTrick(); // also clears currentTrickMatchPlayerSlot internally

        // Step 4: Set next trick leader — which resets trick matchPlayerSlots properly
        game.setTrickLeaderMatchPlayerSlot(winnerMatchPlayerSlot); // internally sets new matchPlayerSlots order
        game.setCurrentMatchPlayerSlot(winnerMatchPlayerSlot);

        log.info(" & New trick lead is matchPlayerSlot {}. New trickMatchPlayerSlotOrder is: {}.",
                winnerMatchPlayerSlot, game.getTrickMatchPlayerSlotOrderAsString());

        // Step 5: Persist state
        gameRepository.save(game);

        log.info(" & Trick transitioned. New currentMatchPlayerSlot: {}. Current trick #: {}.",
                game.getCurrentMatchPlayerSlot(), game.getCurrentTrickNumber());
        log.info(" &&& TRICK COMPLETION CONCLUDED &&&");
    }

    @Transactional
    public void passingAcceptCards(Game game, MatchPlayer matchPlayer, GamePassingDTO passingDTO,
            Boolean pickRandomly) {
        cardPassingService.passingAcceptCards(game, matchPlayer, passingDTO, pickRandomly);
        gameRepository.saveAndFlush(game);
    }

    public Game getActiveGameByMatchId(Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new EntityNotFoundException("Match not found");
        }

        // Always fetch the Game from the database based on match and phase
        Game game = gameRepository.findFirstByMatchAndPhaseNotIn(
                match, List.of(GamePhase.FINISHED, GamePhase.ABORTED));

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

        if (players.size() != 4) {
            throw new IllegalStateException("Expected 4 match players, but found " + players.size());
        }

        for (MatchPlayer player : players) {
            player.resetReady(); // Custom logic
        }
    }
}
