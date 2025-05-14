package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.Strategy;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchSummary;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchPlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.MatchSummaryRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.AIPlayerDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.JoinRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service handling all setup operations for a Match before it starts.
 */
@Service
@Transactional
public class MatchSetupService {

    private final Logger log = LoggerFactory.getLogger(MatchSetupService.class);

    GameRepository gameRepository;
    GameSetupService gameSetupService;
    MatchPlayerRepository matchPlayerRepository;
    MatchRepository matchRepository;
    MatchSummaryRepository matchSummaryRepository;
    UserRepository userRepository;
    UserService userService;

    @Autowired
    public MatchSetupService(
            GameRepository gameRepository,
            GameSetupService gameSetupService,
            MatchPlayerRepository matchPlayerRepository,
            MatchRepository matchRepository,
            MatchSummaryRepository matchSummaryRepository,
            UserRepository userRepository,
            UserService userService) {
        this.gameRepository = gameRepository;
        this.gameSetupService = gameSetupService;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchRepository = matchRepository;
        this.matchSummaryRepository = matchSummaryRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Creates a new match and assigns the host as player 1.
     *
     * @param playerToken authentication token of the host
     * @return a new Match entity
     */
    public Match createNewMatch(String playerToken) {
        User user = userService.getUserByToken(playerToken);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        List<Match> activeMatches = matchRepository.findActiveMatchesByHostId(user.getId());
        if (!activeMatches.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User is already hosting a match. Match ID: " + activeMatches.get(0).getMatchId());
        }

        // Create a MatchSummary
        MatchSummary matchSummary = new MatchSummary();

        Match match = new Match();
        MatchPlayer hostPlayer = new MatchPlayer();
        hostPlayer.setUser(user);
        hostPlayer.setMatch(match);
        hostPlayer.setMatchPlayerSlot(1);
        hostPlayer.setIsHost(true);

        match.setMatchPlayers(List.of(hostPlayer));
        match.setMatchSummary(matchSummary);
        match.setHostId(user.getId());
        match.setHostUsername(user.getUsername());
        match.setMatchGoal(100);
        match.setStarted(false);
        match.setPlayer1(user);

        matchRepository.saveAndFlush(match);

        // Remember player's names for much later
        List<String> playerNames = match.getMatchPlayers().stream()
                .map(mp -> mp.getUser() != null ? mp.getUser().getUsername() : "AI")
                .toList();

        match.setMatchPlayerNames(playerNames);
        return match;
    }

    /**
     * Deletes a match if the requester is its host.
     *
     * @param matchId the ID of the match to delete
     * @param token   the token of the requesting user
     */
    public void deleteMatchByHost(Long matchId, String token) {
        Match match = getMatchOrThrow(matchId);
        User user = userRepository.findUserByToken(token);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        if (!match.getHostId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Only the host can delete matches");
        }

        matchRepository.delete(match);
    }

    /**
     * Validates whether a match is in a state that allows it to be started.
     * 
     * Conditions checked:
     * - Match exists.
     * - Match is not already in progress, finished, or aborted.
     * - The requester is the match host.
     * - All invited users have accepted.
     * - All player slots (1–4) are filled.
     * - No active game already exists.
     *
     * @param match the match to be validated
     * @param user  the user attempting to start the match
     * @throws ResponseStatusException if any condition is violated
     */
    public void isMatchStartable(Match match, User user) {
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        MatchPhase phase = match.getPhase();
        if (phase == MatchPhase.SETUP) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Match is not ready to start. MatchPhase=" + phase);
        }
        if (phase == MatchPhase.IN_PROGRESS || phase == MatchPhase.BETWEEN_GAMES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This match has already been started.");
        }
        if (phase == MatchPhase.ABORTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This match has been cancelled.");
        }
        if (phase == MatchPhase.FINISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This match has already been finished.");
        }

        if (!Objects.equals(user.getId(), match.getHostId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Only the host can start the match.");
        }

        Map<Integer, Long> invites = Optional.ofNullable(match.getInvites()).orElseGet(HashMap::new);
        Map<Long, String> joinRequests = Optional.ofNullable(match.getJoinRequests()).orElseGet(HashMap::new);

        for (Long invitedUserId : invites.values()) {
            String status = joinRequests.get(invitedUserId);
            if (!"accepted".equalsIgnoreCase(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot start match: not all invited users have accepted.");
            }
        }

        if (Stream.of(match.getPlayer1(), match.getPlayer2(), match.getPlayer3(), match.getPlayer4())
                .anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot start match: not all player slots are filled.");
        }

        if (match.getActiveGame() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active game already exists for this match.");
        }
    }

    /**
     * Starts the match by validating its state, resetting stats, and creating the
     * first game.
     *
     * @param matchId the ID of the match to start
     * @param token   the authorization token of the host
     * @param seed    an optional seed for deterministic game setup
     */
    @Transactional
    public void startMatch(Long matchId, String token, Long seed) {
        User user = userService.requireUserByToken(token);
        Match match = requireMatchByMatchId(matchId); // depends on circular structure
        validateMatchIsStartable(match, user);

        match.getMatchPlayers().forEach(MatchPlayer::resetMatchStats);

        Game game = gameSetupService.createAndStartGameForMatch(match, matchRepository, gameRepository, seed);
        gameRepository.save(game);
    }

    /**
     * Updates the match goal (score threshold to end the match).
     *
     * @param matchId the ID of the match
     * @param body    a map containing the \"matchGoal\" key with its new value
     */
    public void updateMatchGoal(Long matchId, Map<String, Integer> body) {
        Match match = getMatchOrThrow(matchId);

        if (!body.containsKey("matchGoal")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'matchGoal' field");
        }

        match.setMatchGoal(body.get("matchGoal"));
        matchRepository.save(match);
    }

    /**
     * Sends an invitation to a user to join a specific slot in the match.
     *
     * @param matchId the ID of the match
     * @param request the invitation details
     */
    public void invitePlayerToMatch(Long matchId, InviteRequestDTO request) {
        Match match = getMatchOrThrow(matchId);
        Long userId = request.getUserId();
        int matchPlayerSlot = request.getPlayerSlot() + 1;

        validateSlotAvailability(match, matchPlayerSlot);
        validateInvitationConditions(match, userId);

        if (match.getInvites() == null) {
            match.setInvites(new HashMap<>());
        }

        match.getInvites().put(matchPlayerSlot, userId);
        matchRepository.save(match);
    }

    /**
     * Accepts or declines an invitation to join a match.
     *
     * @param matchId     the ID of the match
     * @param token       the token of the user responding
     * @param responseDTO the response (accept or decline)
     */
    public void respondToInvite(Long matchId, String token, InviteResponseDTO responseDTO) {
        User user = userRepository.findUserByToken(token.replace("Bearer ", ""));
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        Match match = getMatchOrThrow(matchId);

        Integer matchPlayerSlot = match.getInvites().entrySet().stream()
                .filter(entry -> entry.getValue().equals(user.getId()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No invite found for this user."));

        if (responseDTO.isAccepted()) {
            MatchPlayer matchPlayer = new MatchPlayer();
            matchPlayer.setUser(user);
            matchPlayer.setMatch(match);
            matchPlayer.setMatchPlayerSlot(matchPlayerSlot);
            matchPlayer.setIsHost(false);

            match.getMatchPlayers().add(matchPlayer);
            assignUserToSlot(match, user, matchPlayerSlot);
        }

        match.getInvites().remove(matchPlayerSlot);
        checkAndUpdateMatchPhaseIfReady(match);
        matchRepository.save(match);
    }

    /**
     * Retrieves a match or throws an exception if not found.
     *
     * @param matchId the ID of the match
     * @return the Match entity
     */
    private Match getMatchOrThrow(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
    }

    /**
     * Validates that the given player slot is available.
     *
     * @param match the Match
     * @param slot  the slot to check
     */
    private void validateSlotAvailability(Match match, int slot) {
        if (slot < 2 || slot > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid player slot");
        }

        boolean isTaken = match.getMatchPlayers().stream()
                .anyMatch(mp -> mp.getMatchPlayerSlot() == slot);
        if (isTaken || match.getInvites().containsKey(slot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot already taken or invited");
        }
    }

    /**
     * Validates whether the user is eligible to be invited to a match.
     *
     * @param match  the Match
     * @param userId the ID of the user to invite
     */
    private void validateInvitationConditions(Match match, Long userId) {
        if (match.getInvites().containsValue(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already invited");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (Boolean.TRUE.equals(user.getIsAiPlayer())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot invite AI player");
        }

        if (!user.getStatus().equals(UserStatus.ONLINE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be online");
        }
    }

    /**
     * Cancels an invitation previously sent to a specific slot in the match.
     *
     * @param matchId         the ID of the match
     * @param matchPlayerSlot the player slot to revoke invitation from
     */
    public void cancelInvite(Long matchId, Integer matchPlayerSlot) {
        Match match = getMatchOrThrow(matchId);

        Map<Integer, Long> invites = match.getInvites();
        if (invites == null || !invites.containsKey(matchPlayerSlot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No invite found at this matchPlayerSlot.");
        }

        invites.remove(matchPlayerSlot);
        match.setInvites(invites);
        matchRepository.save(match);
    }

    /**
     * Assigns a user to a specific player slot in the match.
     *
     * @param match the Match
     * @param user  the User to assign
     * @param slot  the player slot
     */
    private void assignUserToSlot(Match match, User user, int slot) {
        switch (slot) {
            case 2 -> match.setPlayer2(user);
            case 3 -> match.setPlayer3(user);
            case 4 -> match.setPlayer4(user);
        }
    }

    /**
     * Updates match phase to READY if all player slots are filled.
     *
     * @param match the Match
     */
    private void checkAndUpdateMatchPhaseIfReady(Match match) {
        if (match.getPlayer1() != null && match.getPlayer2() != null &&
                match.getPlayer3() != null && match.getPlayer4() != null) {
            match.setPhase(MatchPhase.READY);
            log.info("💄 MatchPhase is set to READY.");
        }
    }

    /**
     * Adds an AI player to the match in a specified player slot.
     *
     * @param matchId the ID of the match
     * @param dto     the AI player request containing player slot and difficulty
     */
    public void addAiPlayer(Long matchId, AIPlayerDTO dto) {
        Match match = getMatchOrThrow(matchId);

        int difficulty = Math.max(1, Math.min(dto.getDifficulty(), 3));
        int matchPlayerSlot = Math.max(1, Math.min(dto.getPlayerSlot(), 3)) + 1;

        // Ensure slot is free
        switch (matchPlayerSlot) {
            case 2 -> {
                if (match.getPlayer2() != null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PlayerSlot 1 is already taken.");
            }
            case 3 -> {
                if (match.getPlayer3() != null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PlayerSlot 2 is already taken.");
            }
            case 4 -> {
                if (match.getPlayer4() != null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PlayerSlot 3 is already taken.");
            }
        }

        // Get predefined AI user
        Long aiUserId = (long) (difficulty - 1) * 3 + (matchPlayerSlot - 1);
        if (aiUserId < 1 || aiUserId > 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid AI user ID: " + aiUserId);
        }

        User aiUser = userRepository.findUserById(aiUserId);
        if (aiUser == null || !aiUser.getIsAiPlayer()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI user not found or not an AI");
        }

        // Configure and add MatchPlayer
        MatchPlayer aiPlayer = new MatchPlayer();
        aiPlayer.setMatch(match);
        aiPlayer.setUser(aiUser);
        aiPlayer.setMatchPlayerSlot(matchPlayerSlot);
        aiPlayer.setIsAiPlayer(true);
        aiPlayer.setIsHost(false);

        int strategyIndex = (aiUser.getId() != null)
                ? Math.floorMod(aiUser.getId(), Strategy.values().length)
                : Strategy.values().length - 1;
        aiPlayer.setStrategy(Strategy.values()[strategyIndex]);

        match.getMatchPlayers().add(aiPlayer);

        // Assign to match player slots
        switch (matchPlayerSlot) {
            case 2 -> match.setPlayer2(aiUser);
            case 3 -> match.setPlayer3(aiUser);
            case 4 -> match.setPlayer4(aiUser);
        }

        // Track AI difficulty
        match.getAiPlayers().put(matchPlayerSlot, difficulty);

        // Check for duplicate players
        Set<Long> userIds = Stream.of(match.getPlayer2(), match.getPlayer3(), match.getPlayer4())
                .filter(Objects::nonNull)
                .map(User::getId)
                .collect(Collectors.toSet());

        long assignedCount = Stream.of(match.getPlayer2(), match.getPlayer3(), match.getPlayer4())
                .filter(Objects::nonNull).count();

        if (userIds.size() < assignedCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate AI player detected in slots.");
        }

        checkAndUpdateMatchPhaseIfReady(match);
        matchRepository.save(match);
    }

    /**
     * Removes an AI player from a specific slot in the match.
     *
     * @param matchId the ID of the match
     * @param dto     the AI player removal request containing the player slot
     */
    public void removeAiPlayer(Long matchId, AIPlayerDTO dto) {
        Match match = getMatchOrThrow(matchId);
        int playerSlot = dto.getPlayerSlot(); // 1-based: 1 = player2, 2 = player3, 3 = player4
        int matchPlayerSlot = playerSlot + 1;

        MatchPlayer aiToRemove = match.getMatchPlayers().stream()
                .filter(mp -> mp.getMatchPlayerSlot() == matchPlayerSlot &&
                        mp.getUser() != null &&
                        Boolean.TRUE.equals(mp.getUser().getIsAiPlayer()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("No AI player found at the specified playerSlot %d.", playerSlot)));

        match.getMatchPlayers().remove(aiToRemove);

        // Clear slot reference
        switch (matchPlayerSlot) {
            case 2 -> match.setPlayer2(null);
            case 3 -> match.setPlayer3(null);
            case 4 -> match.setPlayer4(null);
        }

        // Remove difficulty tracking
        if (match.getAiPlayers() != null) {
            match.getAiPlayers().remove(matchPlayerSlot);
        }

        matchRepository.save(match);
    }

    /**
     * Removes a player (human or AI) from a specified player slot in the match.
     *
     * @param matchId    the ID of the match
     * @param playerSlot the player slot (0 = player2, 1 = player3, 2 = player4)
     */
    public void removePlayer(Long matchId, int playerSlot) {
        Match match = getMatchOrThrow(matchId);
        int matchPlayerSlot = playerSlot + 1;

        MatchPlayer toRemove = match.getMatchPlayers().stream()
                .filter(mp -> mp.getMatchPlayerSlot() == matchPlayerSlot)
                .findFirst()
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No player at this playerSlot."));

        match.getMatchPlayers().remove(toRemove);

        // Clear slot reference
        switch (matchPlayerSlot) {
            case 2 -> match.setPlayer2(null);
            case 3 -> match.setPlayer3(null);
            case 4 -> match.setPlayer4(null);
        }

        matchRepository.save(match);
    }

    /**
     * Sends a join request from a user to a match if they are not already in the
     * match
     * and have not previously requested to join.
     *
     * @param matchId the ID of the match
     * @param userId  the ID of the user requesting to join
     */
    public void sendJoinRequest(Long matchId, Long userId) {
        Match match = getMatchOrThrow(matchId);
        User user = requireHumanOnlineAvailableUserByUserId(userId);

        MatchPlayer alreadyInMatch = matchPlayerRepository.findByUserAndMatch(user, match);

        boolean notInMatch = alreadyInMatch == null;
        boolean noPendingRequest = !match.getJoinRequests().containsKey(userId);

        if (notInMatch && noPendingRequest) {
            match.getJoinRequests().put(userId, "pending");
            matchRepository.save(match);
        }
    }

    /**
     * Retrieves a user by ID, ensuring they are online, not an AI, and not already
     * part of a match.
     *
     * @param userId the ID of the user to fetch
     * @return the valid User entity
     * @throws ResponseStatusException if the user doesn't meet the requirements
     */
    public User requireHumanOnlineAvailableUserByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (Boolean.TRUE.equals(user.getIsAiPlayer())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI players cannot join via join request.");
        }

        if (user.getStatus() != UserStatus.ONLINE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be online to join.");
        }

        List<Match> allMatches = matchRepository.findAll();
        boolean isInAnyMatch = allMatches.stream()
                .filter(m -> !m.getStarted()) // only check matches in setup
                .flatMap(m -> m.getMatchPlayers().stream())
                .anyMatch(mp -> user.equals(mp.getUser()));

        if (isInAnyMatch) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already in a match.");
        }

        return user;
    }

    /**
     * Accepts a pending join request for a user and adds them to the match.
     *
     * @param matchId the ID of the match
     * @param userId  the ID of the user whose join request is accepted
     */
    public void acceptJoinRequest(Long matchId, Long userId) {
        Match match = requireMatchByMatchId(matchId);
        User user = requireHumanOnlineAvailableUserByUserId(userId);

        // Ensure the request exists and is pending
        String status = match.getJoinRequests().get(userId);
        if (status == null || !"pending".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending join request for this user.");
        }

        // Find the first available matchPlayerSlot (2-4)
        int availableSlot = Stream.of(2, 3, 4)
                .filter(slot -> getPlayerBySlot(match, slot) == null)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "No free player slots available."));

        // Create new MatchPlayer
        MatchPlayer newPlayer = new MatchPlayer();
        newPlayer.setMatch(match);
        newPlayer.setUser(user);
        newPlayer.setMatchPlayerSlot(availableSlot);
        newPlayer.setIsHost(false);

        // Add to match
        match.getMatchPlayers().add(newPlayer);
        setPlayerBySlot(match, availableSlot, user);

        // Update request status
        match.getJoinRequests().put(userId, "accepted");

        checkAndUpdateMatchPhaseIfReady(match);
        matchRepository.save(match);
    }

    /**
     * Declines a pending join request from a user.
     *
     * @param matchId the ID of the match
     * @param userId  the ID of the user whose request should be declined
     */
    public void declineJoinRequest(Long matchId, Long userId) {
        Match match = requireMatchByMatchId(matchId);

        String status = match.getJoinRequests().get(userId);
        if (status == null || !"pending".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending join request found for this user.");
        }

        match.getJoinRequests().put(userId, "declined");
        matchRepository.save(match);
    }

    /**
     * Retrieves all join requests associated with a match.
     *
     * @param matchId the ID of the match
     * @return a list of JoinRequestDTO objects representing pending or handled
     *         requests
     */
    public List<JoinRequestDTO> getJoinRequests(Long matchId) {
        Match match = requireMatchByMatchId(matchId);

        return match.getJoinRequests().entrySet().stream()
                .map(entry -> {
                    JoinRequestDTO dto = new JoinRequestDTO();
                    dto.setUserId(entry.getKey());
                    dto.setStatus(entry.getValue());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of users who are eligible to join a match.
     * Eligibility criteria:
     * - Must be ONLINE
     * - Must not be an AI user
     * - Must not already be in the match
     * - Must not already be invited to the match
     * - Must not be the requesting user
     *
     * @param matchId the ID of the match
     * @param token   the authorization token of the requesting user
     * @return list of eligible users as DTOs
     */
    public List<UserGetDTO> getEligibleUsers(Long matchId, String token) {
        User currentUser = requireHumanOnlineUserByToken(token);
        Match match = requireMatchByMatchId(matchId);

        Set<Long> excludedUserIds = new HashSet<>();

        // Exclude all currently assigned players
        Stream.of(match.getPlayer1(), match.getPlayer2(), match.getPlayer3(), match.getPlayer4())
                .filter(Objects::nonNull)
                .map(User::getId)
                .forEach(excludedUserIds::add);

        // Exclude invited users
        if (match.getInvites() != null) {
            excludedUserIds.addAll(match.getInvites().values());
        }

        // Exclude the current user themselves
        excludedUserIds.add(currentUser.getId());

        // Get all users who are online and not AI
        List<User> eligibleUsers = userRepository.findByStatusAndIsAiPlayerFalse(UserStatus.ONLINE);

        return eligibleUsers.stream()
                .filter(user -> !excludedUserIds.contains(user.getId()))
                .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
                .collect(Collectors.toList());
    }

    /**
     * Validates whether the given match is in a state that allows it to be started.
     * This includes:
     * - Match exists and is not already started, aborted, or finished.
     * - The requesting user is the host.
     * - All invited users have accepted.
     * - All player slots (1–4) are filled.
     * - No active game already exists.
     *
     * @param match the match to validate
     * @param user  the user requesting to start the match
     * @throws ResponseStatusException if the match cannot be started for any reason
     */
    public void validateMatchIsStartable(Match match, User user) {
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        MatchPhase phase = match.getPhase();
        if (phase == MatchPhase.SETUP) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Match is not ready to start. MatchPhase=" + phase);
        }
        if (phase == MatchPhase.IN_PROGRESS || phase == MatchPhase.BETWEEN_GAMES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This match has already been started.");
        }
        if (phase == MatchPhase.ABORTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This match has been cancelled.");
        }
        if (phase == MatchPhase.FINISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This match has already been finished.");
        }

        if (!Objects.equals(user.getId(), match.getHostId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Only the host can start the match.");
        }

        Map<Integer, Long> invites = Optional.ofNullable(match.getInvites()).orElseGet(HashMap::new);
        Map<Long, String> joinRequests = Optional.ofNullable(match.getJoinRequests()).orElseGet(HashMap::new);

        for (Long invitedUserId : invites.values()) {
            String status = joinRequests.get(invitedUserId);
            if (!"accepted".equalsIgnoreCase(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot start match: not all invited users have accepted the invitation.");
            }
        }

        if (Stream.of(match.getPlayer1(), match.getPlayer2(), match.getPlayer3(), match.getPlayer4())
                .anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot start match: not all player slots are filled.");
        }

        if (match.getActiveGame() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active game already exists for this match.");
        }
    }

    private User getPlayerBySlot(Match match, int slot) {
        return switch (slot) {
            case 2 -> match.getPlayer2();
            case 3 -> match.getPlayer3();
            case 4 -> match.getPlayer4();
            default -> throw new IllegalArgumentException("Invalid slot number");
        };
    }

    private void setPlayerBySlot(Match match, int slot, User user) {
        switch (slot) {
            case 2 -> match.setPlayer2(user);
            case 3 -> match.setPlayer3(user);
            case 4 -> match.setPlayer4(user);
            default -> throw new IllegalArgumentException("Invalid slot number");
        }
    }

    public Match requireMatchByMatchId(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Match not found with id: " + matchId));
    }

    private User requireHumanOnlineUserByToken(String token) {
        User user = userRepository.findUserByToken(token);
        if (user == null || user.getIsAiPlayer() || user.getStatus() != UserStatus.ONLINE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be online and not an AI");
        }
        return user;
    }

}
