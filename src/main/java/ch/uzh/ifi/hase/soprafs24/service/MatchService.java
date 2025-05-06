package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchMessageType;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchMessage;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Match Service
 * This class is the "worker" and responsible for all functionality related to
 * matches
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class MatchService {
    private final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final GameSetupService gameSetupService;
    private final GameSimulationService gameSimulationService;
    private final HtmlSummaryService htmlSummaryService;
    private final MatchPlayerRepository matchPlayerRepository;
    private final PollingService pollingService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MatchRepository matchRepository;
    private final MatchSetupService matchSetupService;

    @Autowired
    public MatchService(
            @Qualifier("gameRepository") GameRepository gameRepository,
            @Qualifier("gameService") GameService gameService,
            @Qualifier("gameSetupService") GameSetupService gameSetupService,
            @Qualifier("gameSimulationService") GameSimulationService gameSimulationService,
            @Qualifier("htmlSummaryService") HtmlSummaryService htmlSummaryService,
            @Qualifier("matchPlayerRepository") MatchPlayerRepository matchPlayerRepository,
            @Qualifier("matchRepository") MatchRepository matchRepository,
            @Qualifier("pollingService") PollingService pollingService,
            @Qualifier("userRepository") UserRepository userRepository,
            @Qualifier("userService") UserService userService,
            @Qualifier("matchSetupService") MatchSetupService matchSetupService) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.gameSetupService = gameSetupService;
        this.gameSimulationService = gameSimulationService;
        this.htmlSummaryService = htmlSummaryService;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchRepository = matchRepository;
        this.pollingService = pollingService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.matchSetupService = matchSetupService;

    }

    public List<Match> getMatchesInformation() {
        return matchRepository.findAll();
    }

    public Match getPolling(Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);

        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match with id " + matchId + " not found");
        }

        return match;
    }

    public int playerSlotToMatchPlayerSlot(Integer i) {
        return (int) (i + 1);
    }

    public int matchPlayerSlotToPlayerSlot(Integer i) {
        return (int) (i - 1);
    }

    public void leaveMatch(Long matchId, String token) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        User user = userRepository.findUserByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        // Don't allow the host to leave
        if (match.getHostId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Host cannot leave the match.");
        }

        // Find the MatchPlayer entry
        MatchPlayer toRemove = match.getMatchPlayers().stream()
                .filter(mp -> mp.getUser().equals(user))
                .findFirst()
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not part of this match."));

        match.getMatchPlayers().remove(toRemove);

        // Remove from matchPlayerSlot reference
        if (match.getPlayer2() != null && match.getPlayer2().equals(user)) {
            match.setPlayer2(null);
        } else if (match.getPlayer3() != null && match.getPlayer3().equals(user)) {
            match.setPlayer3(null);
        } else if (match.getPlayer4() != null && match.getPlayer4().equals(user)) {
            match.setPlayer4(null);
        }

        matchRepository.save(match);
    }

    public void passingAcceptCards(Long matchId, GamePassingDTO passingDTO, String token, Boolean pickRandomly) {
        Match match = requireMatchByMatchId(matchId);
        User user = userService.requireUserByToken(token);
        Game game = gameRepository.findActiveGameByMatchId(match.getMatchId());
        MatchPlayer matchPlayer = match.requireMatchPlayerByUser(user);
        System.out.println("matchService.passingAcceptCards reached");
        gameService.passingAcceptCards(game, matchPlayer, passingDTO, pickRandomly);
    };

    public Match requireMatchByMatchId(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Match not found with id: " + matchId));
    }

    public void playCardAsHuman(String token, Long matchId, PlayedCardDTO dto) {
        // SANITIZE
        Match match = requireMatchByMatchId(matchId);
        Game game = requireActiveGameByMatch(match);
        MatchPlayer matchPlayer = match.requireMatchPlayerByToken(token);
        String cardCode = "";
        if ("XX".equals(dto.getCard())) {
            cardCode = "XX";
        } else {
            cardCode = CardUtils.requireValidCardFormat(dto.getCard());
        }
        gameService.playCardAsHuman(game, matchPlayer, cardCode);
        log.info("MatchService: playCardAsHuman. Now checking if isFinalCardOfGame.");

        // Step 6: Finalize game/match if this was the last card
        if (isFinalCardOfGame(game)) {
            handleGameScoringAndConfirmation(game.getMatch(), game);
        }

    }

    public void playAiTurnsUntilHuman(Match match) {
        Game game = requireActiveGameByMatch(match);

        // Let GameService do the actual AI playing
        gameService.playAiTurnsUntilHuman(game.getGameId());

        log.info("MatchService: playAiTurnsUntilHuman. Now checking if isFinalCardOfGame.");
        // Step 6: Finalize game/match if this was the last card
        if (isFinalCardOfGame(game)) {
            handleGameScoringAndConfirmation(game.getMatch(), game);
        }

    }

    private boolean isFinalCardOfGame(Game game) {
        log.info(
                "MatchService: Now checking if isFinalCardOfGame. GamePhase={}, currentPlayOrder={}.",
                game.getPhase(), game.getCurrentPlayOrder());
        return game.getPhase() == GamePhase.RESULT &&
                game.getCurrentPlayOrder() >= GameConstants.FULL_DECK_CARD_COUNT;
    }

    @Transactional
    public void startMatch(Long matchId, String token, Long seed) {
        User user = userService.requireUserByToken(token);
        Match match = requireMatchByMatchId(matchId);
        matchSetupService.isMatchStartable(match, user);
        log.info("Match is being started (seed=`{}´)", seed);
        match.getMatchPlayers().forEach(MatchPlayer::resetMatchStats);

        Game game = gameSetupService.createAndStartGameForMatch(match, matchRepository, gameRepository, seed);

        gameRepository.save(game);
    }

    public PollingDTO getPlayerPolling(String token, Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
        User user = userRepository.findUserByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        Game game = match.getActiveGame();
        if (game != null) {
            User currentUser = match.requireUserBySlot(game.getCurrentMatchPlayerSlot());
            MatchPlayer requestingPlayer = match.requireMatchPlayerByUser(user);

            boolean isMatchOwnerPolling = requestingPlayer.getMatchPlayerSlot() == 1;

            // Give each polling player a fair chance to grab TrickPhase.JUSTCOMPLETED at
            // least once.
            if (isMatchOwnerPolling) {
                gameService.advanceTrickPhaseIfOwnerPolling(game);
            }
            if (isMatchOwnerPolling &&
                    currentUser != null &&
                    Boolean.TRUE.equals(currentUser.getIsAiPlayer()) &&
                    game.getPhase().inTrick()) {

                playAiTurnsUntilHuman(match);
            }
        }

        return pollingService.getPlayerPolling(user, match, gameRepository, matchPlayerRepository);
    }

    public Game requireActiveGameByMatch(Match match) {
        Game activeGame = gameRepository.findActiveGameByMatchId(match.getMatchId());
        if (activeGame == null) {
            throw new IllegalStateException("No active game found for this match (MatchService).");
        }
        return activeGame;
    }

    public void handleGameScoringAndConfirmation(Match match, Game finishedGame) {
        log.info("MatchService: handleGameScoringAndConfirmation, gamePhase={}.", finishedGame.getPhase());
        // Defensive check (optional)
        if (finishedGame.getPhase() != GamePhase.RESULT) {
            throw new IllegalStateException("Trying to confirm a game that isn't finished.");
        }
        log.info("MatchService: handleGameScoringAndConfirmation.");
        // Apply game score → match score
        for (MatchPlayer mp : match.getMatchPlayers()) {
            // Calculate the updated match score
            int updatedMatchScore = mp.getMatchScore() + mp.getGameScore();
            mp.setMatchScore(updatedMatchScore);
            log.info("MatchService: handleGameScoringAndConfirmation.");
            log.info("MatchPlayer Id={}, slot={} had his MatchScore updated to {}.", mp.getUser().getId(),
                    mp.getMatchPlayerSlot(), updatedMatchScore);

            // Reset game score to 0 after applying it
            mp.setGameScore(0);

            if (mp.getUser() != null) {
                mp.setReady(false); // Set the player as not ready

                // Log for debugging
                log.info("    🫡 MatchPlayer id={} in slot={} was set to ready=false.",
                        mp.getUser().getId(),
                        mp.getMatchPlayerSlot());
            }

            // Save updated MatchPlayer
            matchPlayerRepository.save(mp);
        }

        // Save the updated match after applying the scores
        matchRepository.save(match); // Ensure match is saved with updated matchPlayer scores
        log.info("MatchService: handleGameScoringAndConfirmation. Scores saved in Matchplayer.");

        // Generate HTML summary and message
        String summary = htmlSummaryService.buildMatchResultHtml(match, finishedGame);
        match.setSummary(summary);

        MatchMessage message = new MatchMessage();
        message.setType(MatchMessageType.GAME_ENDED);
        message.setContent("Game finished! Please confirm to continue.");
        match.addMessage(message);

        match.setPhase(MatchPhase.BETWEEN_GAMES);
        log.info("💄 MatchPhase is set to BETWEEN_GAMES.");
        matchRepository.save(match);
    }

    public void handleConfirmedGame(Match match, Game finishedGame) {
        // Ensure all human players have confirmed
        boolean allHumansReady = match.getMatchPlayers().stream()
                .filter(mp -> !Boolean.TRUE.equals(mp.getIsAiPlayer()))
                .allMatch(MatchPlayer::getIsReady);

        if (!allHumansReady) {
            log.info("Waiting for all players to confirm...");
            return;
        }

        // Now that all humans have confirmed, the game is set to finished!
        finishedGame.setPhase(GamePhase.FINISHED);
        log.info("💄 GamePhase is set to FINISHED.");
        gameRepository.save(finishedGame);
        if (shouldEndMatch(match)) {
            match.setPhase(MatchPhase.RESULT);
            log.info("💄 MatchPhase is set to RESULT.");
            matchRepository.save(match);
        } else {
            match.setPhase(MatchPhase.BETWEEN_GAMES);

            log.info("💄 MatchPhase is set to BETWEEN_GAMES.");
            matchRepository.save(match);
            gameSetupService.createAndStartGameForMatch(match, matchRepository, gameRepository, null);
        }
    }

    /**
     * Handles completed Match and deletes all dependencies (games, matchPlayers,
     * messages, passedCards) while keeping the Match itself.
     *
     * @param match the match to shut down
     */
    public void handleConfirmedMatch(Match match) {
        // Mark game as finished
        match.setPhase(MatchPhase.FINISHED);
        log.info("MatchPhase is set to FINISHED.");

        // Clear dependencies manually to trigger orphan removal
        match.getGames().clear();
        match.getMatchPlayers().clear();
        match.getMessages().clear();

        // Optionally clear auxiliary maps/lists
        match.getInvites().clear();
        match.getJoinRequests().clear();
        match.getAiPlayers().clear();

        // Save match with cleared children — children will be deleted
        matchRepository.saveAndFlush(match);
    }

    public void checkGameAndStartNextIfNeeded(Match match) {
        Game activeGame = requireActiveGameByMatch(match);
        if (activeGame != null) {
            return; // There is still an active game
        }

        boolean matchOver = match.getMatchPlayers().stream()
                .anyMatch(mp -> mp.getMatchScore() >= match.getMatchGoal());

        if (matchOver) {
            match.setPhase(MatchPhase.FINISHED);
            log.info("💄 MatchPhase is set to FINISHED.");
            matchRepository.save(match);
        } else {
            match.setPhase(MatchPhase.BETWEEN_GAMES);

            log.info("💄 MatchPhase is set to BETWEEN_GAMES.");
            matchRepository.save(match); // Save transition before creating a new game

            // Delegate to GameSetupService for game creation
            gameSetupService.createAndStartGameForMatch(match, matchRepository, gameRepository, null);
        }
    }

    public void confirmGameResult(String token, Long matchId) {
        Match match = requireMatchByMatchId(matchId);
        Game game = requireActiveGameByMatch(match);
        User user = userRepository.findUserByToken(token);
        if (user == null) {

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        MatchPlayer player = matchPlayerRepository.findByUserAndMatch(user, match);

        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found in match");
        }
        player.setReady(true);

        log.info("    🫡 MatchPlayer id={} in slot={} was set to ready=true.",
                player.getUser().getId(),
                player.getMatchPlayerSlot());
        matchPlayerRepository.save(player);

        boolean allHumansReady = match.getMatchPlayers().stream()
                .filter(mp -> !Boolean.TRUE.equals(mp.getIsAiPlayer()))
                .allMatch(MatchPlayer::getIsReady);

        if (allHumansReady) {
            log.info("    🫡 All human MatchPlayers are Ready!");
            handleConfirmedGame(match, game);
        }
    }

    public boolean shouldEndMatch(Match match) {
        int goal = match.getMatchGoal();
        return match.getMatchPlayers().stream()
                .anyMatch(mp -> mp.getMatchScore() >= goal);
    }

    @Transactional
    public void autoPlayToLastTrick(Long matchId) {
        Match match = requireMatchByMatchId(matchId);
        Game game = requireActiveGameByMatch(match);

        // Call the simulation service to play the game until the last trick
        gameSimulationService.autoPlayToLastTrick(match, game);

        // Log matchPlayer scores before saving
        match.getMatchPlayers().forEach(player -> {
            log.info("MatchPlayer id={}, slot={}, matchScore={}",
                    player.getUser().getId(), player.getMatchPlayerSlot(), player.getMatchScore());
        });

        // Save all match players with the updated scores
        matchPlayerRepository.saveAll(match.getMatchPlayers());

        log.info("Auto-play to last trick finished for match {}", matchId);
    }

}
