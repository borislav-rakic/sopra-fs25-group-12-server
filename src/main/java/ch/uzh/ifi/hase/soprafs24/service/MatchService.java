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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map.Entry;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private ObjectMapper objectMapper;

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
        matchPlayers.get(0).setSlot(1);

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

        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match with id " + matchId + " not found.");
        }

        // Only allow inviting to slots 2, 3, or 4
        if (playerSlot < 2 || playerSlot > 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You can only invite players to slots 2, 3, or 4.");
        }

        // Check if that slot is already taken
        boolean isSlotTaken = match.getMatchPlayers().stream()
                .anyMatch(mp -> mp.getSlot() == playerSlot);

        Map<Integer, Long> invites = match.getInvites();
        if (invites == null) {
            invites = new HashMap<>();
        }

        boolean isSlotInvited = invites.containsKey(playerSlot);

        if (isSlotTaken || isSlotInvited) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot " + playerSlot + " is already occupied.");
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

        invites.put(playerSlot, userId);
        match.setInvites(invites);

        matchRepository.save(match);
    }

    public void cancelInvite(Long matchId, Integer slot) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        Map<Integer, Long> invites = match.getInvites();
        if (invites == null || !invites.containsKey(slot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No invite found at this slot.");
        }

        invites.remove(slot);
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

        Integer slot = null;

        for (Map.Entry<Integer, Long> entry : invites.entrySet()) {
            if (entry.getValue().equals(user.getId())) {
                slot = entry.getKey();
                break;
            }
        }

        if (slot == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No invite found for this user.");
        }

        if (responseDTO.isAccepted()) {
            List<MatchPlayer> players = match.getMatchPlayers();

            MatchPlayer newMatchPlayer = new MatchPlayer();
            newMatchPlayer.setUser(user);
            newMatchPlayer.setMatch(match);
            newMatchPlayer.setSlot(slot);

            players.add(newMatchPlayer);

            match.setMatchPlayers(players);
            if (slot == 2) {
                match.setPlayer2(user);
            } else if (slot == 3) {
                match.setPlayer3(user);
            } else if (slot == 4) {
                match.setPlayer4(user);
            }
        }
        invites.remove(slot);
        match.setInvites(invites);

        // if all four players are ready, we are good to go.
        if (match.getPlayer1() != null
                && match.getPlayer2() != null
                && match.getPlayer3() != null
                && match.getPlayer4() != null) {
            match.setPhase(MatchPhase.READY);
        }

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

    public void addAiPlayer(Long matchId, AIPlayerDTO dto) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        int difficulty = Math.max(1, Math.min(dto.getDifficulty(), 3)); // Difficulty must be within [1,3]
        int slot = dto.getSlot(); // Slot must be: 2 = player2, 3 = player3, 4 = player4

        if (slot < 1 || slot > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AI slot must be between 1 and 3.");
        }

        int playerSlotClient = slot; // slots 1, 2 or 3.
        int playerSlotServer = playerSlotClient + 1; // slots 2, 3 or 4.

        // Check if that slot is already taken
        switch (playerSlotServer) {
            case 2:
                if (match.getPlayer2() != null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot 2 is already taken.");
                break;
            case 3:
                if (match.getPlayer3() != null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot 3 is already taken.");
                break;
            case 4:
                if (match.getPlayer4() != null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot 4 is already taken.");
                break;
        }

        // Get the corresponding AI "user"
        Long aiPlayersId = (long) (difficulty - 1) * 3 + (playerSlotServer - 1);
        if (aiPlayersId < 1 || aiPlayersId > 9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AI user for combination of slot " + playerSlotServer + "  and difficulty " + difficulty
                            + " not found (" + aiPlayersId + ")or is not an AI.");
        }

        User aiPlayer = userRepository.findUserById(aiPlayersId);
        if (aiPlayer == null || !aiPlayer.getIsAiPlayer()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AI user for difficulty " + difficulty + " not found or is not an AI.");
        }

        // Create and assign MatchPlayer
        MatchPlayer newMatchPlayer = new MatchPlayer();
        newMatchPlayer.setMatch(match);
        newMatchPlayer.setUser(aiPlayer);
        newMatchPlayer.setSlot(playerSlotServer);
        newMatchPlayer.setIsAiPlayer(true);

        match.getMatchPlayers().add(newMatchPlayer);

        // Assign to slot in Match
        if (playerSlotServer == 2) {
            match.setPlayer2(aiPlayer);
        } else if (playerSlotServer == 3) {
            match.setPlayer3(aiPlayer);
        } else {
            match.setPlayer4(aiPlayer);
        }

        // Track AI players in the match
        Map<Integer, Integer> aiPlayers = match.getAiPlayers();
        if (aiPlayers == null) {
            aiPlayers = new HashMap<>();
        }
        aiPlayers.put(playerSlotServer, difficulty);
        match.setAiPlayers(aiPlayers);

        matchRepository.save(match);
    }

    public void removeAiPlayer(Long matchId, AIPlayerDTO dto) {
        int slot = dto.getSlot(); // 1-based: 1 = player2, 2 = player3, 3 = player4

        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        int playerSlot = slot + 1; // because player2 is slot 1

        // Find the AI player at the requested slot
        MatchPlayer toRemove = match.getMatchPlayers().stream()
                .filter(mp -> mp.getSlot() == playerSlot && mp.getUser() != null
                        && Boolean.TRUE.equals(mp.getUser().getIsAiPlayer()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No AI player found at the specified slot"));

        // Remove MatchPlayer
        match.getMatchPlayers().remove(toRemove);

        // Remove from match slot
        if (playerSlot == 2)
            match.setPlayer2(null);
        else if (playerSlot == 3)
            match.setPlayer3(null);
        else if (playerSlot == 4)
            match.setPlayer4(null);

        Map<Integer, Integer> aiPlayers = match.getAiPlayers();
        if (aiPlayers != null) {
            aiPlayers.remove(playerSlot); // remove by slot
            match.setAiPlayers(aiPlayers);
        }

        matchRepository.save(match);
    }

    public void removePlayer(Long matchId, int slot) {
        Match match = matchRepository.findMatchByMatchId(matchId);
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        int playerSlot = slot + 1; // Frontend sends 0 = player2, etc.

        MatchPlayer toRemove = match.getMatchPlayers().stream()
                .filter(mp -> mp.getSlot() == playerSlot)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No player at this slot."));

        match.getMatchPlayers().remove(toRemove);

        if (playerSlot == 2) {
            match.setPlayer2(null);
        } else if (playerSlot == 3) {
            match.setPlayer3(null);
        } else if (playerSlot == 4) {
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
            newMatchPlayer.setSlot(2);
        } else if (match.getPlayer3() == null) {
            match.setPlayer3(user);
            newMatchPlayer.setSlot(3);
        } else if (match.getPlayer4() == null) {
            match.setPlayer4(user);
            newMatchPlayer.setSlot(4);
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

        // Remove from slot reference
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

    public void passingAcceptCards(Long matchId, GamePassingDTO passingDTO, String token) {
        Match match = requireMatchByMatchId(matchId);
        User user = userService.requireUserByToken(token);
        Game game = requireActiveGameByMatch(match);
        MatchPlayer matchPlayer = match.requireMatchPlayerByUser(user);
        System.out.println("matchService.passingAcceptCards reached");
        gameService.passingAcceptCards(game, matchPlayer, passingDTO);
    };

    public void playCardAsHuman(String token, Long matchId, PlayedCardDTO dto) {
        User user = userService.requireUserByToken(token);
        Match match = requireMatchByMatchId(matchId);
        Game game = requireActiveGameByMatch(match);
        String cardCode = CardUtils.requireValidCardFormat(dto.getCard());
        MatchPlayer matchPlayer = match.requireMatchPlayerByToken(token);
        int playerSlotServer = dto.getPlayerSlot() + 1; // 1 to account for 0-based frontend counting
        if (matchPlayer.getSlot() != playerSlotServer) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Mismatch between token and slot");
        }
        try {
            log.info("PlayedCardDTO: {}", objectMapper.writeValueAsString(dto));
            log.info(user.getUsername() + " / " + match.getMatchId() + "/" + playerSlotServer);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize PlayedCardDTO", e);
        }
        log.info(String.format(
                "The matchPlayerId=%s (userId=%s, slot=%s), is trying to play the card %s, his hand being %s.",
                matchPlayer.getMatchPlayerId(), matchPlayer.getUser().getId(), playerSlotServer, cardCode,
                matchPlayer.getHand()));

        log.info("======S=L=O=T======");
        List<Integer> slots = game.getCurrentTrickSlots();
        String slotsAsString = slots.stream().map(String::valueOf).collect(Collectors.joining(","));
        log.info("Order of Slots: {}.", slotsAsString);
        game.setTrickLeaderSlot(2);
        List<Integer> slots2 = game.getCurrentTrickSlots();
        String slotsAsString2 = slots2.stream().map(String::valueOf).collect(Collectors.joining(","));
        log.info("Order of Slots2: {}.", slotsAsString2);
        log.info("MUSTDELETE THESE LINES!");

        gameService.playCardAsHuman(game, matchPlayer, cardCode);
    }

    public void startMatch(Long matchId, String token, Long seed) {
        gameService.startMatch(matchId, token, seed);
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
            nextGame.setCurrentSlot(0);
            nextGame.setTrickLeaderSlot(0); // will have to be initialised only after passing
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

}
