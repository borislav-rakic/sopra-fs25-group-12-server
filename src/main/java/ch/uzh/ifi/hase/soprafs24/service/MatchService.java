package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameConstants;
import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchMessageType;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchMessage;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchSummary;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.exceptions.GameplayException;
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

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import ch.uzh.ifi.hase.soprafs24.util.MatchUtils;

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
    private final MatchSummaryService matchSummaryService;
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
            @Qualifier("matchSummaryService") MatchSummaryService matchSummaryService,
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
        this.matchSummaryService = matchSummaryService;
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

    public Match getMatchDTO(Long matchId) {
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

        log.info("matchService.passingAcceptCards reached");
        if (game.getPhase() == GamePhase.SKIP_PASSING) {
            if (passingDTO != null && !passingDTO.getCards().isEmpty()) {
                throw new GameplayException("No cards should be passed during SKIP_PASSING.");
            }

            matchPlayer.setReady(true);
            matchPlayerRepository.saveAndFlush(matchPlayer);
            return;
        }

        gameService.passingAcceptCards(game, matchPlayer, passingDTO, pickRandomly);
    };

    public Match requireMatchByMatchId(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Match not found with id: " + matchId));
    }

    public void playCardAsHuman(String token, Long matchId, PlayedCardDTO dto) {
        Match match = requireMatchByMatchId(matchId);
        Game game = requireActiveGameByMatch(match);
        MatchPlayer matchPlayer = match.requireMatchPlayerByToken(token);

        String cardCode = "XX".equals(dto.getCard()) ? "XX" : CardUtils.requireValidCardFormat(dto.getCard());
        gameService.playCardAsHuman(game, matchPlayer, cardCode);

    }

    public void wrapUpCompletedGame(Game game) {
        Match match = game.getMatch();

        // Create and store match summary
        String summary = matchSummaryService.buildMatchResultHtml(match, game);
        setExistingMatchSummaryOrCreateIt(match, summary);

        // Add game ended message
        MatchMessage message = new MatchMessage();
        message.setType(MatchMessageType.GAME_ENDED);
        message.setContent("Game finished! Please confirm to continue.");
        match.addMessage(message);

        // Decide next match phase
        if (shouldEndMatch(match)) {
            match.setPhase(MatchPhase.RESULT);
            log.info("💄 MatchPhase is set to RESULT.");
        } else {
            match.setPhase(MatchPhase.BETWEEN_GAMES);
            log.info("💄 MatchPhase is set to BETWEEN_GAMES.");
        }

        matchRepository.save(match);
    }

    @Transactional
    public void startMatch(Long matchId, String token, Long seed) {
        User user = userService.requireUserByToken(token);
        Match match = requireMatchByMatchId(matchId);
        matchSetupService.isMatchStartable(match, user);
        log.info("Match is being started (seed=`{}´)", seed);
        match.getMatchPlayers().forEach(MatchPlayer::resetMatchStats);
        // make sure the game has been polled fresh.
        getHostMatchPlayer(match).updateLastPollTime();

        Game game = gameSetupService.createAndStartGameForMatch(match, matchRepository, gameRepository, seed);

        gameRepository.save(game);
    }

    public PollingDTO getPlayerPolling(String token, Long matchId) {
        // Match available?
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
        // User identfiable?
        User requestingUser = userRepository.findUserByToken(token);
        if (requestingUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        // --- Handle match already in RESULT or FINISHED phase ---
        if (match.getPhase() == MatchPhase.RESULT
                || match.getPhase() == MatchPhase.FINISHED
                || match.getPhase() == MatchPhase.ABORTED) {
            Integer slot = getSlotForUser(match, requestingUser);

            boolean isMatchPlayer = slot != null;

            boolean hasConfirmed = slot != null && match.getSlotDidConfirmLastGame().contains(slot);

            boolean showGameResult = match.getPhase() == MatchPhase.RESULT
                    || (match.getPhase() == MatchPhase.FINISHED && isMatchPlayer && !hasConfirmed);

            return pollingService.getPlayerPollingForPostMatchPhase(
                    requestingUser,
                    match,
                    showGameResult);
        }

        // Who is polling?
        // Is it an actual MatchPlayer of this match?
        MatchPlayer requestingMatchPlayer = match.getMatchPlayers().stream()
                .filter(mp -> mp.getUser().getId().equals(requestingUser.getId()))
                .findFirst()
                .orElse(null);

        // No! It is a random person.
        if (requestingMatchPlayer == null) {
            return pollingService.getSpectatorPolling(requestingUser, match);
        }

        // Yes! Let us remember their visit.
        requestingMatchPlayer.updateLastPollTime();
        matchPlayerRepository.save(requestingMatchPlayer);

        // Are we still fine about the host being alive?
        if (secondsSinceHostsLastPolling(match) > GameConstants.HOST_TIME_OUT_SECONDS) {
            log.info("Host is not polling anymore.");
            abortMatch(match);
        }

        // Is there an active game for this match?
        Game game = match.getActiveGame();
        if (game != null) {
            // Whose turn is it anyway?
            MatchPlayer currentPlayer = match.requireMatchPlayerBySlot(game.getCurrentMatchPlayerSlot());

            // Is the person calling the host of the match?
            if (requestingMatchPlayer.getIsHost()) {

                if (game.getPhase() == GamePhase.SKIP_PASSING) {
                    assertAllHumanPlayersSkippedPassing(match, game);
                }

                // Advance the TrickPhase if this user is the host
                gameService.advanceTrickPhaseIfOwnerPolling(game);

                // After trick phase advancement, check if the match should end
                if (match.getPhase().inGame() && shouldEndMatch(match)) {
                    game.setPhase(GamePhase.FINISHED);
                    gameRepository.save(game);

                    match.setPhase(MatchPhase.RESULT);
                    setExistingMatchSummaryOrCreateIt(match,
                            matchSummaryService.buildMatchResultHtml(match, game));
                    matchRepository.saveAndFlush(match);
                    log.info("MatchPhase set to RESULT — match has ended.");
                    // return immediately!
                    return pollingService.getPlayerPollingForPostMatchPhase(
                            requestingUser,
                            match,
                            true // showGameResult
                    );
                }

                // Is the currentPlayer an AIPlayer who is supposed to play a card
                if (
                // The match is not over yet.
                !shouldEndMatch(match)
                        // The game is still on.
                        && game.getPhase().inTrick()
                        // The TrickPhase is just fine.
                        && (game.getTrickPhase() == TrickPhase.READYFORFIRSTCARD
                                || game.getTrickPhase() == TrickPhase.RUNNINGTRICK)
                        // I really am an AIPlayer and it it is my turn
                        && Boolean.TRUE.equals(currentPlayer.getIsAiPlayer())) {

                    gameService.playSingleAiTurn(match, game, currentPlayer);

                    // Having done that, let us check if the game is perhaps over.
                    if (gameService.finalizeGameIfComplete(game)) {
                        wrapUpCompletedGame(game); // Already defined in MatchService
                    }
                }
            }
        }
        // Every MatchPlayer needs their polling (host or non-host).
        return pollingService.getPlayerPolling(requestingUser, match, gameRepository, matchPlayerRepository);
    }

    /**
     * Finds active game for given match or throws
     * 
     * @param match Current Match object.
     * @return Game object of current match.
     * @throws IllegalStateException if there is no active game for this match at
     *                               this point.
     */
    public Game requireActiveGameByMatch(Match match) {
        Game activeGame = gameRepository.findActiveGameByMatchId(match.getMatchId());
        if (activeGame == null) {
            throw new IllegalStateException("No active game found for this match (MatchService).");
        }
        return activeGame;
    }

    /**
     * Checks if all human players of a game are ready again after
     * GamePhase=SKIP_PASSING.
     * 
     * @param match current Match
     * @param game  current Game
     */

    private void assertAllHumanPlayersSkippedPassing(Match match, Game game) {
        if (game.getPhase() == GamePhase.SKIP_PASSING
                && game.getTrickPhase() == TrickPhase.READYFORFIRSTCARD
                && MatchUtils.verifyAllHumanMatchPlayersReady(match)) {

            gameService.assignTwoOfClubsLeader(game);
            game.setCurrentTrickNumber(1);
            game.setCurrentPlayOrder(0);
            game.setPhase(GamePhase.FIRSTTRICK);
            game.setTrickPhase(TrickPhase.READYFORFIRSTCARD);
            log.info("/// READY TO PLAY FIRST TRICK (AFTER SKIPPING PASSING) ///");

            gameRepository.saveAndFlush(game);
        }
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
        match.setPhase(MatchPhase.BETWEEN_GAMES);

        log.info("💄 MatchPhase is set to BETWEEN_GAMES.");
        matchRepository.save(match);
        gameSetupService.createAndStartGameForMatch(match, matchRepository, gameRepository, null);
    }

    /**
     * Handles completed Match and deletes all dependencies (games, matchPlayers,
     * messages, passedCards) while keeping the Match itself.
     *
     * @param match the match to shut down
     */
    @Transactional
    public void handleMatchInResultPhaseOrAborted(Match match) {
        log.info("MatchPhase is set to FINISHED.");

        // Sever parent-child references for games so orphan removal works
        for (Game game : match.getGames()) {
            game.setMatch(null);
        }
        match.getGames().clear();

        for (MatchPlayer player : match.getMatchPlayers()) {
            player.setMatch(null);
        }
        match.getMatchPlayers().clear();

        for (MatchMessage message : match.getMessages()) {
            message.setMatch(null);
        }
        match.getMessages().clear();

        match.getInvites().clear();
        match.getJoinRequests().clear();
        match.getAiPlayers().clear();

        matchRepository.saveAndFlush(match);
    }

    public void checkGameAndStartNextIfNeeded(Match match) {
        // If the match is already in RESULT or FINISHED, nothing to do
        if (!match.getPhase().equals(MatchPhase.BETWEEN_GAMES)) {
            return;
        }

        // Defensive: make sure no active game exists before creating a new one
        Game activeGame = requireActiveGameByMatch(match);
        if (activeGame != null) {
            log.info("Active game still present, not starting new one.");
            return;
        }

        // Start next game
        log.info("Starting next game in BETWEEN_GAMES phase.");
        gameSetupService.createAndStartGameForMatch(match, matchRepository, gameRepository, null);
    }

    public void confirmGameResult(String token, Long matchId) {
        Match match = requireMatchByMatchId(matchId);
        User user = userRepository.findUserByToken(token);
        if (user == null) {
            log.info("Unknown or unauthorized user. Confirmation ignored.");
            return;
        }

        MatchPhase phase = match.getPhase();

        // Early exit if no confirmation needed
        if (phase == MatchPhase.FINISHED || phase == MatchPhase.ABORTED) {
            log.info("Match already finished or aborted. No further confirmation needed.");
            return;
        }

        // Handle match result confirmation
        if (phase == MatchPhase.RESULT) {
            Integer slot = getSlotForUser(match, user);
            if (slot == null) {
                log.info("User {} is not a match participant. Ignoring.", user.getId());
                return;
            }

            if (!match.getSlotDidConfirmLastGame().contains(slot)) {
                match.getSlotDidConfirmLastGame().add(slot);
                log.info("MatchPlayer in slot {} confirmed match result.", slot);

                // Transition to FINISHED after first confirmation
                match.setPhase(MatchPhase.FINISHED);
                matchRepository.save(match);
                log.info("Match marked as FINISHED.");
            }
            return;
        }

        // Handle normal game confirmation
        Game game = requireActiveGameByMatch(match);
        MatchPlayer player = matchPlayerRepository.findByUserAndMatch(user, match);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found in match");
        }

        player.setReady(true);
        matchPlayerRepository.save(player);
        log.info("MatchPlayer id={} in slot={} set to ready.", user.getId(), player.getMatchPlayerSlot());

        boolean allHumansReady = match.getMatchPlayers().stream()
                .filter(mp -> !Boolean.TRUE.equals(mp.getIsAiPlayer()))
                .allMatch(MatchPlayer::getIsReady);

        if (allHumansReady) {
            log.info("All human players confirmed game result.");
            handleConfirmedGame(match, game);
        }
    }

    private Integer getSlotForUser(Match match, User user) {
        if (user.getId().equals(match.getPlayer1().getId()))
            return 1;
        if (user.getId().equals(match.getPlayer2().getId()))
            return 2;
        if (user.getId().equals(match.getPlayer3().getId()))
            return 3;
        if (user.getId().equals(match.getPlayer4().getId()))
            return 4;
        return null;
    }

    public boolean shouldEndMatch(Match match) {
        int goal = match.getMatchGoal();
        return match.getMatchPlayers().stream()
                .anyMatch(mp -> mp.getMatchScore() >= goal);
    }

    @Transactional
    public void autoPlayToLastTrickOfGame(Long matchId, Integer fakeShootingTheMoon) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        Game game = match.getActiveGame();
        gameSimulationService.autoPlayToLastTrickOfGame(match, game, fakeShootingTheMoon);
    }

    @Transactional
    public void autoPlayToGameSummary(Long matchId, Integer fakeShootingTheMoon) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        Game game = match.getActiveGame();
        gameSimulationService.autoPlayToGameSummary(match, game);
    }

    @Transactional
    public void autoPlayToMatchSummary(Long matchId, Integer fakeShootingTheMoon) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        Game game = match.getActiveGame();
        gameSimulationService.autoPlayToMatchSummary(match, game);
    }

    @Transactional
    public void autoPlayToLastTrickOfMatchThree(Long matchId, Integer fakeShootingTheMoon) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        Game game = match.getActiveGame();
        gameSimulationService.autoPlayToLastTrickOfMatchThree(match, game);
    }

    @Transactional
    public void autoPlayToLastTrickOfMatch(Long matchId) {
        Match match = requireMatchByMatchId(matchId);
        Game game = requireActiveGameByMatch(match);

        // Call the simulation service to play the game until the last trick
        gameSimulationService.autoPlayToLastTrickOfMatch(match, game);

        // Log matchPlayer scores before saving
        match.getMatchPlayers().forEach(player -> {
            log.info("MatchPlayer id={}, slot={}, matchScore={}",
                    player.getUser().getId(), player.getMatchPlayerSlot(), player.getMatchScore());
        });

        // Save all match players with the updated scores
        matchPlayerRepository.saveAll(match.getMatchPlayers());

        log.info("Auto-play to last trick finished for match {}", matchId);
    }

    public void setExistingMatchSummaryOrCreateIt(Match match, String matchSummaryHtml) {
        MatchSummary matchSummary = match.getMatchSummary();

        if (matchSummary == null) {
            matchSummary = new MatchSummary();
            match.setMatchSummary(matchSummary); // Ensure bidirectional consistency
        }

        if (matchSummaryHtml != null) {
            matchSummary.setMatchSummaryHtml(matchSummaryHtml);
        }

        // Persist the match (and MatchSummary if cascade is enabled)
        matchRepository.save(match);
    }

    public MatchPlayer getHostMatchPlayer(Match match) {
        Long hostId = match.getHostId();

        for (MatchPlayer player : match.getMatchPlayers()) {
            if (player.getUser() != null && player.getUser().getId().equals(hostId)) {
                return player;
            }
        }

        return null; // host's MatchPlayer not found
    }

    public int secondsSinceHostsLastPolling(Match match) {
        MatchPlayer hostPlayer = getHostMatchPlayer(match);
        if (hostPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match does not have a host.");
        }
        return (int) Duration.between(hostPlayer.getLastPollTime(), Instant.now()).getSeconds();
    }

    public void abortMatch(Match match) {
        match.setPhase(MatchPhase.ABORTED);
        match.getMatchSummary().setMatchSummaryHtml(
                "<div>The Host Player was offline for more than 30 seconds. The match was disbanded before completion.</div>");
        handleMatchInResultPhaseOrAborted(match);
    }
}
