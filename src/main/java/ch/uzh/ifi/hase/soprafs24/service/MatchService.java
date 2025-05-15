package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.AiMatchPlayerState;
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
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import ch.uzh.ifi.hase.soprafs24.util.MatchUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Match Service
 * This class is the "worker" and responsible for all functionality related to
 * matches (e.g., it creates, modifies, deletes, finds). The result will be
 * passed back to the caller.
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

    public MatchDTO getMatchDTO(Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        MatchDTO dto = DTOMapper.INSTANCE.convertEntityToMatchDTO(match);

        // Manually add playerNames
        dto.setPlayerNames(match.getMatchPlayerNames());

        dto.setSlotAvailable(match.getPlayer1() == null
                || match.getPlayer2() == null
                || match.getPlayer3() == null
                || match.getPlayer4() == null);

        return dto;
    }

    /*
     * Convert playerSlot (0-3) to matchPlayerSlot (1-4)
     * 
     * @param i playerSlot
     * 
     * @return corresponding matchPlayerSlot
     */
    public int playerSlotToMatchPlayerSlot(Integer i) {
        return (int) (i + 1);
    }

    /*
     * Convert matchPlayerSlot to playerSlot
     * 
     * @param i matchPlayerSlot
     * 
     * @return corresponding playerSlot
     */
    public int matchPlayerSlotToPlayerSlot(Integer i) {
        return (int) (i - 1);
    }

    /**
     * Replaces current host of match with other human MatchPlayer or aborts Match.
     * 
     * @param match Match Object in which a change of host should take place.
     */
    @Transactional
    public void findNewHumanHostOrAbortMatch(Match match) {
        // 1. Identify current host
        Long previousHostId = match.getHostId();
        User previousHostUser = userRepository.findUserById(previousHostId);
        if (previousHostUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not identify match host.");
        }

        MatchPlayer previousHostMatchPlayer = matchPlayerRepository.findByUserAndMatch(previousHostUser, match);
        if (previousHostMatchPlayer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not identify hosting MatchPlayer.");
        }

        // 2. Find a new human host (other than the current one)
        MatchPlayer newHumanHostMatchPlayer = null;
        for (MatchPlayer mp : match.getMatchPlayers()) {
            if (mp.getIsAiPlayer()) {
                // only human players are eligible
                continue;
            }

            if (mp.getUser().getId().equals(previousHostId)) {
                // old host is not eligible, either
                continue;
            }
            Duration durationSinceLastPulse = Duration.between(mp.getLastPollTime(), Instant.now());
            if (durationSinceLastPulse.toSeconds() > 10) {
                // this user has not polled in more than ten seconds,
                // so that user is not a good choice
                continue;
            }
            newHumanHostMatchPlayer = mp;
            break;
        }

        if (newHumanHostMatchPlayer == null) {
            // no human MatchPlayer proved worthy of the role
            abortMatch(match);
            return;
        }

        // 3. Transfer host responsibility
        previousHostMatchPlayer.setIsHost(false);
        newHumanHostMatchPlayer.setIsHost(true);
        match.setHostId(newHumanHostMatchPlayer.getUser().getId());
        match.setHostUsername(newHumanHostMatchPlayer.getUser().getUsername());

        gameService.relayMessageToMatchMessageService(match, MatchMessageType.PLAYER_LEFT,
                previousHostUser.getUsername());
        gameService.relayMessageToMatchMessageService(match, MatchMessageType.HOST_CHANGED,
                newHumanHostMatchPlayer.getUser().getUsername());

        // 4. Replace former host with AI player in their slot
        int slotToBeReplaced = previousHostMatchPlayer.getMatchPlayerSlot();
        replaceMatchPlayerSlotWithAiPlayer(match, slotToBeReplaced); // ← Delegated to utility method

        // 5. Persist updated host
        matchPlayerRepository.save(newHumanHostMatchPlayer);
        matchRepository.save(match);

        log.info("Host transferred from MatchPlayerSlot {} to {}.",
                slotToBeReplaced, newHumanHostMatchPlayer.getMatchPlayerSlot());
    }

    /**
     * Finds the UserId:Long of a currently unemployed AI Player in this match
     * 
     * @param match Match in which the AI Player might need to particpate.
     * @return Long with UserId of available AI Player.
     */
    public Long findUserIdOfUnoccupiedAiPlayer(Match match) {
        List<MatchPlayer> mps = match.getMatchPlayers();

        // Collect all user IDs already in use
        Set<Long> usedIds = mps.stream()
                .map(mp -> mp.getUser().getId())
                .collect(Collectors.toSet());

        // Check for the first unoccupied AI player ID (1 to 9)
        for (long candidate = 1L; candidate < 10L; candidate++) {
            if (!usedIds.contains(candidate)) {
                return candidate;
            }
        }

        // No available AI user ID
        return null;
    }

    /**
     * A human MatchPlayer needs to be replaced by an AI Player.
     * 
     * @param match the Current Match Object.
     * @matchPlayerSlot the position of the player that is replaced.
     */

    @Transactional
    public void replaceMatchPlayerSlotWithAiPlayer(Match match, int matchPlayerSlot) {
        // Find unoccupied AI Player
        Long newAiPlayerId = findUserIdOfUnoccupiedAiPlayer(match);
        User newAiUser = userRepository.findUserById(newAiPlayerId);
        if (newAiUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No available AI player.");
        }

        // Find the MatchPlayer by slot
        MatchPlayer replaced = match.getMatchPlayers().stream()
                .filter(mp -> mp.getMatchPlayerSlot() == matchPlayerSlot)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No player in that slot."));

        // Swap in AI player
        replaced.setUser(newAiUser);
        replaced.setIsAiPlayer(true);
        replaced.setAiMatchPlayerState(AiMatchPlayerState.READY);
        replaced.setIsHost(false); // Ensure AI doesn't remain host accidentally

        // Update player reference in Match
        switch (matchPlayerSlot) {
            case 1:
                match.setPlayer1(newAiUser);
                break;
            case 2:
                match.setPlayer2(newAiUser);
                break;
            case 3:
                match.setPlayer3(newAiUser);
                break;
            case 4:
                match.setPlayer4(newAiUser);
                break;
            default:
                throw new IllegalArgumentException("Invalid MatchPlayerSlot: " + matchPlayerSlot);
        }

        // Notify system
        gameService.relayMessageToMatchMessageService(match, MatchMessageType.PLAYER_JOINED, newAiUser.getUsername());
        // update AI Player's name in list of names for match.
        match.setNameForMatchPlayerSlot(matchPlayerSlot, newAiUser.getUsername());
        matchPlayerRepository.save(replaced);
        matchRepository.save(match);

        log.info("Slot {} replaced with AI Player (UserId={}).", matchPlayerSlot, newAiUser.getId());
    }

    /**
     * Deal with a user signalling they want to leave the Match.
     *
     * @param matchId ID of the match the user wants to leave.
     * @param token   Authentication token of the user.
     */
    @Transactional
    public void leaveMatch(Long matchId, String token, User identifiedUser) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        // If an identified user was passed into the function
        // then there is no need to check the token.

        final User user = (identifiedUser != null)
                ? identifiedUser
                : userRepository.findUserByToken(token);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
        // Host wants to leave
        if (match.getHostId().equals(user.getId())) {
            if (!GameConstants.HOSTS_ARE_ALLOWED_TO_LEAVE_THE_MATCH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Host cannot leave the match.");
            }
            findNewHumanHostOrAbortMatch(match);
            return;
        }

        // Regular player wants to leave → replace with AI
        MatchPlayer leavingMatchPlayer = match.getMatchPlayers().stream()
                .filter(mp -> mp.getUser().equals(user))
                .findFirst()
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not part of this match."));

        int slot = leavingMatchPlayer.getMatchPlayerSlot();
        replaceMatchPlayerSlotWithAiPlayer(match, slot);

        gameService.relayMessageToMatchMessageService(match, MatchMessageType.PLAYER_LEFT, user.getUsername());

        log.info("Player at slot {} left the match and was replaced by an AI player.", slot);
    }

    /**
     * Processes cards sent for passing
     * 
     * @param matchId      ID of match
     * @param passingDTO   Cards that are to be passed
     * @param token        Token identifying user
     * @param pickRandomly if set to true overrides any passed card of the
     *                     passingDTO and selects three cards at random
     */

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

    /**
     * Returns Match object to given matchId or throws
     * 
     * @param matchId
     * @returns Match object
     * @throws ResponseStatusException if matchId could not be identified.
     */
    public Match requireMatchByMatchId(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Match not found with id: " + matchId));
    }

    /**
     * Play the given card as part of the given match identified by matchId
     * 
     * @param token   Token identifying user.
     * @param matchId ID of releveant match.
     * @param dto     PlayedCardDTO with played card.
     */
    public void playCardAsHuman(String token, Long matchId, PlayedCardDTO dto) {
        Match match = requireMatchByMatchId(matchId);
        Game game = requireActiveGameByMatch(match);
        MatchPlayer matchPlayer = match.requireMatchPlayerByToken(token);

        String cardCode = "XX".equals(dto.getCard()) ? "XX" : CardUtils.requireValidCardFormat(dto.getCard());
        try {
            gameService.playCardAsHuman(game, matchPlayer, cardCode);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Processes a completed game
     * 
     * @param game Game object of game that was just completed
     */
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
            awardScoresToUsersOfFinishedMatch(match);
            log.info("💄 MatchPhase is set to RESULT.");
        } else {
            match.setPhase(MatchPhase.BETWEEN_GAMES);
            log.info("💄 MatchPhase is set to BETWEEN_GAMES.");
        }

        matchRepository.save(match);
    }

    /**
     * Establishes the rank of MatchPlayers in a match and saves it in
     * the MatchPlayer entity.
     * 
     * @param match the Match object
     */
    public void establishRankingOfMatchPlayersInMatch(Match match) {
        List<MatchPlayer> players = match.getMatchPlayers();

        // Sort players by ascending score
        List<MatchPlayer> sorted = players.stream()
                .sorted(Comparator.comparingInt(MatchPlayer::getGameScore))
                .toList();

        int rank = 1;
        int currentScore = -1;
        int currentRank = 1;

        for (MatchPlayer player : sorted) {
            int score = player.getGameScore();

            if (score != currentScore) {
                currentScore = score;
                currentRank = rank;
            }

            player.setRankingInMatch(currentRank);
            matchPlayerRepository.save(player);

            rank++;
        }

        log.info("Established ranking for {} match players.", players.size());
    }

    /**
     * Awards scores to users of a finished match
     * 
     * @param match Relevant Match object
     */
    public void awardScoresToUsersOfFinishedMatch(Match match) {
        establishRankingOfMatchPlayersInMatch(match);
        // MATCH-related points
        // A.
        // First place in match: 10 points (ex aequo same)
        // Second place in match: 6 points
        // Third place in match: 3 points
        // Fourth place in match: -1 point
        // B. AI Bonus
        // Winning (only first place) against at least one difficult AI player: +4
        // Winning (only first place) against at least one medium AI player +2
        // C. Perfect match
        // Scoring 0 points in an entire match: + 20 points
        // D. Number of matches played
        // E. Average match ranking

        for (MatchPlayer matchPlayer : match.getMatchPlayers()) {
            int newlyGainedPoints = 0;
            User user = matchPlayer.getUser();
            int ranking = matchPlayer.getRankingInMatch();
            // A. Points for Ranking
            if (ranking == 1) {
                newlyGainedPoints += 10.0f;
            } else if (ranking == 2) {
                newlyGainedPoints += 6.0f;
            } else if (ranking == 3) {
                newlyGainedPoints += 2.0f;
            } else if (ranking == 4) {
                newlyGainedPoints += 1.0f;
            }
            // B. AI bonus, only for human players
            if (matchPlayer.getIsAiPlayer()) {
                newlyGainedPoints += highestLevelOfAiPlayers(match) * 2.0f;
            }

            // C. Points for perfect match
            if (matchPlayer.getMatchScore() == 0) {
                user.setPerfectMatches(user.getPerfectMatches() + 1);
                newlyGainedPoints += 20.0f;
            }
            // D. number of matches played
            int oldMatchesPlayed = user.getMatchesPlayed();
            int newMatchesPlayed = oldMatchesPlayed + 1;
            user.setMatchesPlayed(newMatchesPlayed);

            // E. Average Match Ranking
            float oldAverageMatchRanking = 1.0f * user.getAvgMatchRanking();
            float newAverageMatchRanking = 1.0f * (oldMatchesPlayed * oldAverageMatchRanking + ranking)
                    / newMatchesPlayed;
            user.setAvgMatchRanking(newAverageMatchRanking);

            // F. Current Match Streak and Longest Match Streak
            if (ranking == 1) {
                user.setCurrentMatchStreak(user.getCurrentMatchStreak() + 1);
                if (user.getCurrentMatchStreak() > user.getLongestMatchStreak()) {
                    user.setLongestMatchStreak(user.getCurrentMatchStreak());
                }
            } else {
                user.setCurrentMatchStreak(0);
            }
            // G. Update Total Score
            user.setScoreTotal(user.getScoreTotal() + newlyGainedPoints);
            userRepository.save(user);
        }

    }

    public int highestLevelOfAiPlayers(Match match) {
        int highestLevel = 0;

        for (MatchPlayer matchPlayer : match.getMatchPlayers()) {
            if (matchPlayer.getIsAiPlayer()) {
                long userId = matchPlayer.getUser().getId();
                int aiLevel;

                if (userId >= 1 && userId <= 3) {
                    aiLevel = 0; // easy
                } else if (userId >= 4 && userId <= 6) {
                    aiLevel = 1; // medium
                } else if (userId >= 7 && userId <= 9) {
                    aiLevel = 2; // difficult
                } else {
                    aiLevel = 0; // default/fallback
                }

                if (aiLevel > highestLevel) {
                    highestLevel = aiLevel;
                }
            }
        }

        return highestLevel;
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
            Integer slot = getMatchPlayerSlotForUser(match, requestingUser);

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

        if (requestingMatchPlayer.getIsHost()) {
            // Only host is in charge of feeling other human player's pulse.
            feelAllHumanNonHostMatchPlayersPulse(match);
        }

        // Are we still fine about the host being alive?
        if (secondsSinceHostsLastPolling(match) > GameConstants.HOST_TIME_OUT_SECONDS) {
            log.info("Host is not polling anymore.");
            findNewHumanHostOrAbortMatch(match);
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
                        return pollingService.getPlayerPollingForPostMatchPhase(
                                requestingUser,
                                match,
                                true // showGameResult
                        );

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
     * messages, passedCards etc.) while keeping the Match and MatchSummary itself.
     *
     * @param match the match to shut down
     */
    @Transactional
    public void handleMatchInResultPhaseOrAborted(Match match) {
        log.info("MatchPhase is set to FINISHED.");
        cleanupAndOptionallyDeleteMatch(match, false);
    }

    /**
     * Deletes a match if the requester is its host.
     *
     * @param matchId the ID of the match to delete
     * @param token   the token of the requesting user
     */
    public void deleteMatchByHost(Long matchId, String token) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found.");
        }
        User user = userRepository.findUserByToken(token);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        if (match.getHostId() == null || !match.getHostId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Only the host can delete matches");
        }
        cleanupAndOptionallyDeleteMatch(match, true);
    }

    /**
     * Cleans up all associated entities of a Match such as games, match players,
     * messages, invites, join requests, AI players, and stats.
     * Optionally deletes the Match entity itself after cleanup.
     *
     * @param match       the Match to clean up
     * @param deleteMatch if true, deletes the match after cleanup
     */
    @Transactional
    public void cleanupAndOptionallyDeleteMatch(Match match, boolean deleteMatch) {
        log.info("Starting cleanup for Match ID {}", match.getMatchId());

        // 1. Sever child references from Games
        if (match.getGames() != null) {
            for (Game game : match.getGames()) {
                game.setMatch(null);
            }
            match.getGames().clear();
            log.info("Cleared games for Match {}", match.getMatchId());
        }

        // 2. Remove MatchPlayers
        for (MatchPlayer player : match.getMatchPlayers()) {
            player.setMatch(null);
        }
        match.getMatchPlayers().clear();
        log.info("Cleared match players for Match {}", match.getMatchId());

        // 3. Remove Messages
        for (MatchMessage message : match.getMessages()) {
            message.setMatch(null);
        }
        match.getMessages().clear();
        log.info("Cleared messages for Match {}", match.getMatchId());

        // 4. Remove Invites
        if (match.getInvites() != null) {
            match.getInvites().clear();
        }

        // 5. Remove Join Requests
        if (match.getJoinRequests() != null) {
            match.getJoinRequests().clear();
        }

        // 6. Remove AI Players tracking
        if (match.getAiPlayers() != null) {
            match.getAiPlayers().clear();
        }

        // 7. Remove Stats
        gameService.clearAllMatchStatsForGame(match); // Optional but helpful
        log.info("Cleared stats for Match {}", match.getMatchId());

        matchRepository.saveAndFlush(match);
        log.info("Match {} saved and flushed after cleanup.", match.getMatchId());

        if (deleteMatch) {
            matchRepository.delete(match);
            log.info("Match {} has been deleted.", match.getMatchId());
        }
    }

    /**
     * Starts the next game in a match if the match is in BETWEEN_GAMES phase
     * and no active game is currently running.
     *
     * Does nothing if the match is not in BETWEEN_GAMES or if an active game
     * exists.
     *
     * @param match the match to check and possibly start a new game for
     */
    public void checkGameAndStartNextIfNeeded(Match match) {
        if (!match.getPhase().equals(MatchPhase.BETWEEN_GAMES)) {
            return;
        }

        Game activeGame = requireActiveGameByMatch(match);
        if (activeGame != null) {
            log.info("Active game still present, not starting new one.");
            return;
        }

        log.info("Starting next game in BETWEEN_GAMES phase.");
        gameSetupService.createAndStartGameForMatch(match, matchRepository, gameRepository, null);
    }

    /**
     * Confirms the result of the current or last game for a user in a match.
     * 
     * - If the match is in RESULT phase, the user confirms the final result,
     * and the match transitions to FINISHED.
     * - If the match is in a normal game phase, the user is marked as ready.
     * If all human players are ready, the match progresses.
     *
     * @param token   the authentication token of the user
     * @param matchId the ID of the match
     */
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
            Integer slot = getMatchPlayerSlotForUser(match, user);
            if (slot == null) {
                log.info("User {} is not a match participant. Ignoring.", user.getId());
                return;
            }

            if (!match.getSlotDidConfirmLastGame().contains(slot)) {
                match.addSlotDidConfirm(slot);
                log.info("MatchPlayer in slot {} confirmed match result.", slot);

                // Transition to FINISHED after first confirmation
                match.setPhase(MatchPhase.FINISHED);
                matchRepository.save(match);
                log.info("Match marked as FINISHED.");
                handleMatchInResultPhaseOrAborted(match);
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

    private Integer getMatchPlayerSlotForUser(Match match, User user) {
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

    public void autoPlayFastForwardPoints(Long matchId, int pts) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        List<MatchPlayer> mps = match.getMatchPlayersSortedBySlot();
        for (int i = 0; i < mps.size(); i++) {
            MatchPlayer mp = mps.get(i);
            mp.setMatchScore(pts);
            matchPlayerRepository.save(mp);
        }

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

    /**
     * Only match host should check if all MatchPlayers are still polling.
     * If last polling is longer ago than NON_HOST_TIME_OUT_SECONDS,
     * MatchPlayer gets replaced by AI Player.
     * 
     * @param match Relevant Match object.
     **/
    private void feelAllHumanNonHostMatchPlayersPulse(Match match) {
        for (MatchPlayer mp : match.getMatchPlayers()) {
            if (!mp.getIsAiPlayer() && !mp.getIsHost()) {
                Duration durationSinceLastPulse = Duration.between(mp.getLastPollTime(), Instant.now());
                if (durationSinceLastPulse.toSeconds() > GameConstants.NON_HOST_TIME_OUT_SECONDS) {
                    log.info(
                            "MatchPlayerId={} in Match={} (MatchPlayerSlot={}) has not polled in more than {} s and is replaced by an AI Player.",
                            mp.getMatchPlayerId(),
                            match.getMatchId(),
                            mp.getMatchPlayerSlot(),
                            GameConstants.NON_HOST_TIME_OUT_SECONDS);
                    replaceMatchPlayerSlotWithAiPlayer(match, mp.getMatchPlayerSlot());
                }
            }
        }
    }
}
