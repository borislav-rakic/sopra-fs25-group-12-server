package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
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

import java.util.Map.Entry;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
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
    private final MatchPlayerRepository matchPlayerRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MatchRepository matchRepository;

    @Autowired
    public MatchService(
            @Qualifier("cardRulesService") CardRulesService cardRulesService,
            @Qualifier("gameRepository") GameRepository gameRepository,
            @Qualifier("gameService") GameService gameService,
            @Qualifier("matchPlayerRepository") MatchPlayerRepository matchPlayerRepository,
            @Qualifier("matchRepository") MatchRepository matchRepository,
            @Qualifier("userRepository") UserRepository userRepository,
            @Qualifier("userService") UserService userService) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.matchPlayerRepository = matchPlayerRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.userService = userService;

    }

    /**
     * Creates a new {@link Match} entity for the user associated with the provided
     * token.
     *
     * <p>
     * This method:
     * <ul>
     * <li>Validates the provided player token by retrieving the corresponding
     * {@link User}.</li>
     * <li>Creates a new {@link Match} and assigns the user as the host and
     * player1.</li>
     * <li>Initializes a list of {@link MatchPlayer} with the user.</li>
     * <li>Saves the newly created match to the database.</li>
     * </ul>
     *
     * <p>
     * The returned {@link Match} will be transformed into a {@link MatchDTO}
     * elsewhere, where the following fields are populated:
     * </p>
     * <ul>
     * <li>{@code matchId} – Unique identifier of the match.</li>
     * <li>{@code matchPlayerIds} – List of player IDs participating in the
     * match.</li>
     * <li>{@code hostId} – The User Id of the player who created the match.</li>
     * <li>{@code matchGoal} – Default match goal (set to 100).</li>
     * <li>{@code started} – Initially {@code false}, indicating the match hasn't
     * begun.</li>
     * <li>{@code player1Id} – The ID of the initiating player (same as the token
     * owner).</li>
     * </ul>
     *
     * @param playerToken the authentication token of the player creating the match
     * @return a newly created {@link Match} object that can be transformed into a
     *         {@link MatchDTO}
     * @throws ResponseStatusException if the token is invalid or the user cannot be
     *                                 found
     */
    public Match createNewMatch(String playerToken) {
        User user = userService.getUserByToken(playerToken);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        // Check if the user is already hosting an active match (that hasn't started)
        Match activeMatch = matchRepository.findByHostIdAndStarted(user.getId(), false);

        if (activeMatch != null) {
            // If the user is already hosting a match, return the matchId of the active
            // match
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User is already hosting a match. Match ID: " + activeMatch.getMatchId());
        }

        Match match = new Match();

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(new MatchPlayer());
        matchPlayers.get(0).setUser(user);
        matchPlayers.get(0).setMatch(match);
        matchPlayers.get(0).setMatchPlayerSlot(1);

        match.setMatchPlayers(matchPlayers);
        match.setHostId(user.getId());
        match.setHostUsername(user.getUsername());
        match.setMatchGoal(100);
        match.setStarted(false);
        match.setPlayer1(user);

        matchRepository.save(match);
        matchRepository.flush();

        return match;
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

    public void deleteMatchByHost(Long matchId, String token) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        User user = userRepository.findUserByToken(token);

        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        Long userId = user.getId();

        if (!match.getHostId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Only the host can delete matches");
        }

        matchRepository.delete(match);
    }

    public void invitePlayerToMatch(Long matchId, InviteRequestDTO request) {
        Long userId = request.getUserId();
        Integer playerSlot = request.getPlayerSlot();
        int matchPlayerSlot = playerSlot + 1;

        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match with id " + matchId + " not found.");
        }
        log.info("Invitation received: %s.", request);
        // Only allow inviting to matchPlayerSlots 2, 3, or 4
        if (matchPlayerSlot < 2 || matchPlayerSlot > 4) {
            log.info("Invitation received, but it is to illegal matchPlayerSlot %d.", matchPlayerSlot);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Invitations can only go to playerSlot 1, 2, or 3 (received %d).",
                            matchPlayerSlot));
        }

        // Check if that matchPlayerSlot is already taken
        boolean isMatchPlayerSlotTaken = match.getMatchPlayers().stream()
                .anyMatch(mp -> mp.getMatchPlayerSlot() == matchPlayerSlot);

        Map<Integer, Long> invites = match.getInvites();
        if (invites == null) {
            invites = new HashMap<>();
        }

        boolean isMatchPlayerSlotInvited = invites.containsKey(matchPlayerSlot);

        if (isMatchPlayerSlotTaken || isMatchPlayerSlotInvited) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "MatchPlayerSlot " + matchPlayerSlot + " is already occupied.");
        }

        int currentRealPlayers = match.getMatchPlayers().stream()
                .filter(mp -> mp.getUser() != null && !Boolean.TRUE.equals(mp.getUser().getIsAiPlayer()))
                .toList()
                .size();
        int currentAIPlayers = match.getAiPlayers() != null ? match.getAiPlayers().size() : 0;

        if (currentRealPlayers + currentAIPlayers >= 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot invite more players — match is full.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        if (user.getId().equals(match.getHostId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hosts cannot invite themselves.");
        }

        if (invites.containsValue(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has already been invited to this match.");
        }

        if (Boolean.TRUE.equals(user.getIsAiPlayer())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot invite an AI player through this API.");
        }

        if (user.getStatus() != UserStatus.ONLINE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be online to receive an invite.");
        }

        invites.put(matchPlayerSlot, userId);
        match.setInvites(invites);

        matchRepository.save(match);
    }

    public void cancelInvite(Long matchId, Integer matchPlayerSlot) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        Map<Integer, Long> invites = match.getInvites();
        if (invites == null || !invites.containsKey(matchPlayerSlot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No invite found at this matchPlayerSlot.");
        }

        invites.remove(matchPlayerSlot);
        match.setInvites(invites);

        matchRepository.save(match);
    }

    public void respondToInvite(Long matchId, String authHeader, InviteResponseDTO responseDTO) {
        String token = authHeader.replace("Bearer ", "");
        User user = userRepository.findUserByToken(token);

        // Automatically leave other match if joined
        List<Match> allMatches = matchRepository.findAll();
        for (Match m : allMatches) {
            if (m.getStarted())
                continue;
            if (m.getMatchPlayers().stream().anyMatch(mp -> mp.getUser().equals(user))) {
                m.getMatchPlayers().removeIf(mp -> mp.getUser().equals(user));

                if (user.equals(m.getPlayer2()))
                    m.setPlayer2(null);
                if (user.equals(m.getPlayer3()))
                    m.setPlayer3(null);
                if (user.equals(m.getPlayer4()))
                    m.setPlayer4(null);

                matchRepository.save(m);
                break;
            }
        }

        Match match = matchRepository.findMatchByMatchId(matchId);

        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match with id " + matchId + " not found");
        }

        Map<Integer, Long> invites = match.getInvites();

        Integer matchPlayerSlot = null;

        for (Map.Entry<Integer, Long> entry : invites.entrySet()) {
            if (entry.getValue().equals(user.getId())) {
                matchPlayerSlot = entry.getKey();
                break;
            }
        }

        if (matchPlayerSlot == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No invite found for this user.");
        }

        if (responseDTO.isAccepted()) {
            List<MatchPlayer> players = match.getMatchPlayers();

            MatchPlayer newMatchPlayer = new MatchPlayer();
            newMatchPlayer.setUser(user);
            newMatchPlayer.setMatch(match);
            newMatchPlayer.setMatchPlayerSlot(matchPlayerSlot);

            players.add(newMatchPlayer);

            match.setMatchPlayers(players);
            if (matchPlayerSlot == 2) {
                match.setPlayer2(user);
            } else if (matchPlayerSlot == 3) {
                match.setPlayer3(user);
            } else if (matchPlayerSlot == 4) {
                match.setPlayer4(user);
            }
        }
        invites.remove(matchPlayerSlot);
        match.setInvites(invites);

        checkAndUpdateMatchPhaseIfReady(match);
        matchRepository.save(match);
    }

    public void updateMatchGoal(Long matchId, Map<String, Integer> body) {
        Match match = matchRepository.findMatchByMatchId(matchId);

        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        if (!body.containsKey("matchGoal")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'matchGoal' field");
        }

        match.setMatchGoal(body.get("matchGoal"));
        matchRepository.save(match);
    }

    public int playerSlotToMatchPlayerSlot(Integer i) {
        return (int) (i + 1);
    }

    public int matchPlayerSlotToPlayerSlot(Integer i) {
        return (int) (i - 1);
    }

    public void addAiPlayer(Long matchId, AIPlayerDTO dto) {
        Match match = requireMatchByMatchId(matchId);
        int difficulty = Math.max(1, Math.min(dto.getDifficulty(), 3)); // Difficulty must be within [1,3]
        // Frontend uses playerSlot 1–3 (never 0), which maps to matchPlayerSlot 2–4
        int matchPlayerSlot = Math.max(1, Math.min(dto.getPlayerSlot(), 3)) + 1;
        // playerSlot must be: 1, 2, 3
        // matchPlayerSlot must be 2, 3, 4

        // Check if that matchPlayerSlot is already taken
        switch (matchPlayerSlot) {
            case 2:
                if (match.getPlayer2() != null)
                    // The exception references client logic playerSlots.
                    // Note: playerSlot 1 = matchPlayerSlot 2.
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PlayerSlot 1 is already taken.");
                break;
            case 3:
                if (match.getPlayer3() != null)
                    // The exception references client logic playerSlots.
                    // Note: playerSlot 2 = matchPlayerSlot 3.
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PlayerSlot 2 is already taken.");
                break;
            case 4:
                if (match.getPlayer4() != null)
                    // The exception references client logic playerSlots.
                    // Note: playerSlot 3 = matchPlayerSlot 4.
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PlayerSlot 3 is already taken.");
                break;
        }

        // Get the corresponding AI "user"
        Long aiPlayersId = (long) (difficulty - 1) * 3 + (matchPlayerSlot - 1);
        if (aiPlayersId < 1 || aiPlayersId > 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format(
                            "AI user for combination of int playerSlot %s  and difficulty %d not found (%d)or is not an AI.",
                            matchPlayerSlot, difficulty, aiPlayersId));
        }

        User aiPlayer = userRepository.findUserById(aiPlayersId);
        if (aiPlayer == null || !aiPlayer.getIsAiPlayer()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AI user for difficulty " + difficulty + " not found or is not an AI.");
        }

        // Create and assign MatchPlayer.
        MatchPlayer newMatchPlayer = new MatchPlayer();
        newMatchPlayer.setMatch(match);
        newMatchPlayer.setUser(aiPlayer);
        newMatchPlayer.setMatchPlayerSlot(matchPlayerSlot);
        newMatchPlayer.setIsAiPlayer(true);

        match.getMatchPlayers().add(newMatchPlayer);

        // Assign to matchPlayerSlot in Match
        if (matchPlayerSlot == 2) {
            match.setPlayer2(aiPlayer);
        } else if (matchPlayerSlot == 3) {
            match.setPlayer3(aiPlayer);
        } else {
            match.setPlayer4(aiPlayer);
        }

        // Track AI players in the match
        Map<Integer, Integer> aiPlayers = match.getAiPlayers();
        if (aiPlayers == null) {
            aiPlayers = new HashMap<>();
        }
        aiPlayers.put(matchPlayerSlot, difficulty);
        match.setAiPlayers(aiPlayers);
        Set<Long> userIdsInSlots = new HashSet<>();

        if (match.getPlayer2() != null)
            userIdsInSlots.add(match.getPlayer2().getId());
        if (match.getPlayer3() != null)
            userIdsInSlots.add(match.getPlayer3().getId());
        if (match.getPlayer4() != null)
            userIdsInSlots.add(match.getPlayer4().getId());

        // Count how many non-null players we actually have.
        long totalAssigned = Stream.of(match.getPlayer2(), match.getPlayer3(), match.getPlayer4())
                .filter(Objects::nonNull)
                .count();

        // If we have more assigned slots than unique users, there must be a duplicate.
        if (userIdsInSlots.size() < totalAssigned) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate AI player detected in slots.");
        }
        checkAndUpdateMatchPhaseIfReady(match);
        matchRepository.save(match);
    }

    public void removeAiPlayer(Long matchId, AIPlayerDTO dto) {
        int playerSlot = dto.getPlayerSlot(); // 1-based: 1 = player2, 2 = player3, 3 = player4
        int matchPlayerSlot = playerSlot + 1;

        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        // Find the AI player at the requested matchPlayerSlot
        MatchPlayer toRemove = match.getMatchPlayers().stream()
                .filter(mp -> mp.getMatchPlayerSlot() == matchPlayerSlot && mp.getUser() != null
                        && Boolean.TRUE.equals(mp.getUser().getIsAiPlayer()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No AI player found at the specified playerSlot %d.", playerSlot)));

        // Remove MatchPlayer
        match.getMatchPlayers().remove(toRemove);

        // Remove from matchPlayerSlot
        if (matchPlayerSlot == 2)
            match.setPlayer2(null);
        else if (matchPlayerSlot == 3)
            match.setPlayer3(null);
        else if (matchPlayerSlot == 4)
            match.setPlayer4(null);

        Map<Integer, Integer> aiPlayers = match.getAiPlayers();
        if (aiPlayers != null) {
            aiPlayers.remove(matchPlayerSlot); // remove by matchPlayerSlot
            match.setAiPlayers(aiPlayers);
        }

        matchRepository.save(match);
    }

    public void removePlayer(Long matchId, int playerSlot) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        int matchPlayerSlot = playerSlot + 1; // Frontend sends 0 = player2, etc.

        MatchPlayer toRemove = match.getMatchPlayers().stream()
                .filter(mp -> mp.getMatchPlayerSlot() == matchPlayerSlot)
                .findFirst()
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No player at this playerSlot."));

        match.getMatchPlayers().remove(toRemove);

        if (matchPlayerSlot == 2) {
            match.setPlayer2(null);
        } else if (matchPlayerSlot == 3) {
            match.setPlayer3(null);
        } else if (matchPlayerSlot == 4) {
            match.setPlayer4(null);
        }

        matchRepository.save(match);
    }

    public void sendJoinRequest(Long matchId, Long userId) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        User user = userRepository.findUserById(userId);

        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        MatchPlayer userToMatchPlayer = matchPlayerRepository.findByUserAndMatch(user, match);

        // Ensure the user isn't already in the match or hasn't already sent a request
        if (userToMatchPlayer == null && !match.getJoinRequests().containsKey(userId)) {
            match.getJoinRequests().put(userId, "pending"); // Add the user to joinRequests
            matchRepository.save(match); // Save the match object with updated joinRequests
        }
    }

    public void acceptJoinRequest(Long matchId, Long userId) {
        Match match = matchRepository.findMatchByMatchId(matchId);

        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
        if (match.getPlayer2() != null)

            match.getJoinRequests().put(userId, "accepted");

        User user = userRepository.findUserById(userId);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        MatchPlayer newMatchPlayer = new MatchPlayer();
        newMatchPlayer.setUser(user);
        newMatchPlayer.setMatch(match);

        match.getMatchPlayers().add(newMatchPlayer);

        if (match.getPlayer2() == null) {
            match.setPlayer2(user);
            newMatchPlayer.setMatchPlayerSlot(2);
        } else if (match.getPlayer3() == null) {
            match.setPlayer3(user);
            newMatchPlayer.setMatchPlayerSlot(3);
        } else if (match.getPlayer4() == null) {
            match.setPlayer4(user);
            newMatchPlayer.setMatchPlayerSlot(4);
        }

        matchRepository.save(match);
    }

    public void declineJoinRequest(Long matchId, Long userId) {
        Match match = matchRepository.findMatchByMatchId(matchId);

        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        match.getJoinRequests().put(userId, "declined");
        matchRepository.save(match);
    }

    public Match requireMatchByMatchId(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Match not found with id: " + matchId));
    }

    public List<JoinRequestDTO> getJoinRequests(Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        List<JoinRequestDTO> joinRequestDTOs = new ArrayList<>();
        for (Entry<Long, String> entry : match.getJoinRequests().entrySet()) {
            JoinRequestDTO dto = new JoinRequestDTO();
            dto.setUserId(entry.getKey());
            dto.setStatus(entry.getValue());
            joinRequestDTOs.add(dto);
        }
        return joinRequestDTOs;
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

    public List<UserGetDTO> getEligibleUsers(Long matchId, String token) {
        User currentUser = userService.getUserByToken(token); // Get the current user

        Match match = getPolling(matchId); // Assuming this method exists

        Set<Long> excludedUserIds = new HashSet<>();

        // Add current match players (if they exist)
        if (match.getPlayer1() != null)
            excludedUserIds.add(match.getPlayer1().getId());
        if (match.getPlayer2() != null)
            excludedUserIds.add(match.getPlayer2().getId());
        if (match.getPlayer3() != null)
            excludedUserIds.add(match.getPlayer3().getId());
        if (match.getPlayer4() != null)
            excludedUserIds.add(match.getPlayer4().getId());

        // Add invited users
        if (match.getInvites() != null) {
            excludedUserIds.addAll(match.getInvites().values());
        }

        // Add current user themselves
        excludedUserIds.add(currentUser.getId());

        // QUESTION: Should we also remove players that are currently active in a game?

        // Get all online, non-AI users
        List<User> eligibleUsers = userRepository.findByStatusAndIsAiPlayerFalse(UserStatus.ONLINE);

        // Filter out excluded IDs
        return eligibleUsers.stream()
                .filter(user -> !excludedUserIds.contains(user.getId()))
                .map(DTOMapper.INSTANCE::convertEntityToUserGetDTO)
                .collect(Collectors.toList());
    }

    /**
     * Throws unless the given match can be started.
     * 
     * @param match The match to be started.
     * @param user  The owner of the match.
     */
    private void isMatchStartable(Match match, User user) {
        // Is the match in a startable MathPhase?
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        } else if (match.getPhase() == MatchPhase.SETUP) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Match is not ready to start. MatchPhase=" + match.getPhase());
        }
        if (match.getPhase() == MatchPhase.IN_PROGRESS
                || match.getPhase() == MatchPhase.BETWEEN_GAMES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This match has already been started.");
        } else if (match.getPhase() == MatchPhase.ABORTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This match has been cancelled by the match owner.");
        } else if (match.getPhase() == MatchPhase.FINISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This match has already been finished.");
        }
        // Is the Match Owner behind this request?
        if (!user.getId().equals(match.getHostId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Only the host can start the match");
        }

        // Collect invites.
        Map<Integer, Long> invites = match.getInvites();
        if (invites == null) {
            invites = new HashMap<>();
        }

        // Collect join requests.
        Map<Long, String> joinRequests = match.getJoinRequests();
        if (joinRequests == null) {
            joinRequests = new HashMap<>();
        }

        // Check if any one of the invites has not yet been accepted.
        for (Long invitedUserId : invites.values()) {
            String status = joinRequests.get(invitedUserId);
            if (!"accepted".equalsIgnoreCase(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot start match: not all invited users have accepted the invitation.");
            }
        }

        // Make sure that all slots are occupied.
        if (match.getPlayer1() == null || match.getPlayer2() == null ||
                match.getPlayer3() == null || match.getPlayer4() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot start match: not all player slots are filled");
        }

        // Prevent creating a new game if an active one already exists.
        if (match.getActiveGame() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active game already exists for this match.");
        }
    }

    private void checkAndUpdateMatchPhaseIfReady(Match match) {
        if (match.getPlayer1() != null &&
                match.getPlayer2() != null &&
                match.getPlayer3() != null &&
                match.getPlayer4() != null) {
            match.setPhase(MatchPhase.READY);
        }
    }

    public void passingAcceptCards(Long matchId, GamePassingDTO passingDTO, String token, Boolean pickRandomly) {
        Match match = requireMatchByMatchId(matchId);
        User user = userService.requireUserByToken(token);
        Game game = requireActiveGameByMatch(match);
        MatchPlayer matchPlayer = match.requireMatchPlayerByUser(user);
        System.out.println("matchService.passingAcceptCards reached");
        gameService.passingAcceptCards(game, matchPlayer, passingDTO, pickRandomly);
    };

    public void playCardAsHuman(String token, Long matchId, PlayedCardDTO dto) {
        // SANITIZE
        Match match = requireMatchByMatchId(matchId);
        Game game = requireActiveGameByMatch(match);
        MatchPlayer matchPlayer = match.requireMatchPlayerByToken(token);
        int matchPlayerSlot = dto.getPlayerSlot() + 1; // 1 to account for 0-based frontend counting
        if (matchPlayer.getMatchPlayerSlot() != matchPlayerSlot) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Mismatch between token and matchPlayerSlot");
        }
        String cardCode = "";
        if (dto.getCard() == "XX") {
            cardCode = "XX";
        } else {
            cardCode = CardUtils.requireValidCardFormat(dto.getCard());
        }
        gameService.playCardAsHuman(game, matchPlayer, cardCode);
    }

    public Match validateMatchReadyToStart(Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        } else if (match.getPhase() == MatchPhase.SETUP) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Match is not ready to start.");
        } else if (match.getPhase().inGame()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Match is already in progress.");
        } else if (match.getPhase().over()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Match is already over.");
        }
        return match;
    }

    @Transactional
    public void startMatch(Long matchId, String token, Long seed) {
        User user = userService.requireUserByToken(token);
        Match match = requireMatchByMatchId(matchId);
        isMatchStartable(match, user);

        match.getMatchPlayers().forEach(MatchPlayer::resetMatchStats);

        gameService.createAndStartGameForMatch(match, seed);
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
        PollingDTO playerPolling = gameService.getPlayerPolling(user, match);
        return playerPolling;
    }

    public Game requireActiveGameByMatch(Match match) {
        Game activeGame = match.getActiveGame();
        if (activeGame == null) {
            throw new IllegalStateException("No active game found for this match.");
        }
        return activeGame;
    }

    public GameResultDTO checkGameAndStartNextIfNeeded(Match match) {
        Game activeGame = match.getActiveGame();
        GameResultDTO result = gameService.concludeGame(activeGame);

        boolean matchOver = match.getMatchPlayers().stream()
                .anyMatch(mp -> mp.getMatchScore() >= match.getMatchGoal());

        if (!matchOver) {
            Game nextGame = new Game();
            nextGame.setMatch(match);
            nextGame.setGameNumber(match.getGames().size() + 1);
            nextGame.setPhase(GamePhase.PASSING);
            nextGame.setCurrentMatchPlayerSlot(1);
            nextGame.setTrickLeaderMatchPlayerSlot(1); // will have to be initialised only after passing
            nextGame.setCurrentPlayOrder(1);
            nextGame.setCurrentTrickNumber(1);
            nextGame.setHeartsBroken(false);
            gameRepository.save(nextGame);

            match.getGames().add(nextGame);
            matchRepository.save(match);
        } else {
            match.setPhase(MatchPhase.FINISHED);
            matchRepository.save(match);
        }

        return result;
    }

    public void confirmGameResult(String token, Long matchId) {
        Match match = requireMatchByMatchId(matchId);
        Game game = requireActiveGameByMatch(match);

        boolean allHumansReady = match.getMatchPlayers().stream()
                .filter(mp -> !Boolean.TRUE.equals(mp.getUser().getIsAiPlayer()))
                .allMatch(mp -> mp.getIsReady());

        if (allHumansReady) {
            game.setPhase(GamePhase.FINISHED);
            gameRepository.save(game);

            if (shouldEndMatch(match)) {
                match.setPhase(MatchPhase.FINISHED);
            } else {
                match.setPhase(MatchPhase.BETWEEN_GAMES);
                Long seed = null;
                String seed_prefix = ExternalApiClientService.SEED_PREFIX;
                if (game.getDeckId().startsWith(seed_prefix)) {
                    String numberPart = game.getDeckId().substring(seed_prefix.length());
                    seed = Long.parseLong(numberPart);
                }
                gameService.createAndStartGameForMatch(match, seed);
                match.setPhase(MatchPhase.BETWEEN_GAMES);
            }

            matchRepository.save(match);
        }
    }

    public boolean shouldEndMatch(Match match) {
        int goal = match.getMatchGoal();
        return match.getMatchPlayers().stream()
                .anyMatch(mp -> mp.getGameScore() >= goal);
    }

}
