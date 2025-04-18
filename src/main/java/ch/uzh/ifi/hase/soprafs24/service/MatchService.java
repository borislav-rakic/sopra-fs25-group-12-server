package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    // private final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final MatchRepository matchRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final MatchPlayerRepository matchPlayerRepository;

    @Autowired
    public MatchService(
            @Qualifier("matchRepository") MatchRepository matchRepository,
            UserService userService,
            @Qualifier("userRepository") UserRepository userRepository,
            @Qualifier("matchPlayerRepository") MatchPlayerRepository matchPlayerRepository) {
        this.matchRepository = matchRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.matchPlayerRepository = matchPlayerRepository;
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
     * <li>{@code host} – The username of the player who created the match.</li>
     * <li>{@code length} – Default match length (set to 100).</li>
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

        Match match = new Match();

        List<MatchPlayer> matchPlayers = new ArrayList<>();
        matchPlayers.add(new MatchPlayer());
        matchPlayers.get(0).setPlayerId(user);
        matchPlayers.get(0).setMatch(match);
        matchPlayers.get(0).setSlot(1);

        match.setMatchPlayers(matchPlayers);
        match.setHost(user.getUsername());
        match.setLength(100);
        match.setStarted(false);
        match.setPlayer1(user);

        matchRepository.save(match);
        matchRepository.flush();

        System.out.println("MATCHID: " + match.getMatchId());

        System.out.println("PLAYERID: " + match.getMatchPlayers().get(0).getPlayerId());

        return match;
    }

    public List<Match> getMatchesInformation() {
        return matchRepository.findAll();
    }

    public Match getMatchInformation(Long matchId) {
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

        String userName = user.getUsername();

        if (!match.getHost().equals(userName)) {
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

        // 1. Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        // 2. Prevent self-inviting
        if (user.getUsername().equals(match.getHost())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hosts cannot invite themselves.");
        }

        // 3. Prevent duplicate invites
        Map<Integer, Long> invites = match.getInvites();
        if (invites == null) {
            invites = new HashMap<>();
        } else if (invites.containsValue(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has already been invited to this match.");
        }

        // 4. Prevent inviting AI
        if (Boolean.TRUE.equals(user.getIsAiPlayer())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot invite an AI player through this API.");
        }

        // 5. Check user is online
        if (user.getStatus() != UserStatus.ONLINE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User must be online to receive an invite.");
        }

        // 6. Save the invite
        invites.put(playerSlot, userId);
        match.setInvites(invites);

        matchRepository.save(match);
    }

    public void respondToInvite(Long matchId, String authHeader, InviteResponseDTO responseDTO) {
        String token = authHeader.replace("Bearer ", "");
        User user = userRepository.findUserByToken(token);

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

        if (responseDTO.isAccepted()) {
            List<MatchPlayer> players = match.getMatchPlayers();

            MatchPlayer newMatchPlayer = new MatchPlayer();
            newMatchPlayer.setPlayerId(user);
            newMatchPlayer.setMatch(match);

            players.add(newMatchPlayer);

            match.setMatchPlayers(players);
            if (slot == 1) {
                match.setPlayer2(user);
            } else if (slot == 2) {
                match.setPlayer3(user);
            } else if (slot == 3) {
                match.setPlayer4(user);
            }
        }
        invites.remove(slot);
        match.setInvites(invites);

        matchRepository.save(match);
    }

    public void updateMatchLength(Long matchId, Map<String, Integer> body) {
        Match match = matchRepository.findMatchByMatchId(matchId);

        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }

        if (!body.containsKey("length")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'length' field");
        }

        match.setLength(body.get("length"));
        matchRepository.save(match);
    }

    public void addAiPlayer(Long matchId, AIPlayerDTO dto) {
        Match match = matchRepository.findMatchByMatchId(matchId);

        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match with id " + matchId + " not found");
        }

        List<Integer> aiPlayers = match.getAiPlayers();
        if (aiPlayers == null) {
            aiPlayers = new ArrayList<>();
        }

        User aiPlayer = userRepository.findUserById(dto.getDifficulty() + 1L);

        MatchPlayer newMatchPlayer = new MatchPlayer();
        newMatchPlayer.setMatch(match);
        newMatchPlayer.setPlayerId(aiPlayer);

        if (match.getPlayer2() == null) {
            match.setPlayer2(userRepository.findUserById(dto.getDifficulty() + 1L));
            newMatchPlayer.setSlot(2);
        } else if (match.getPlayer3() == null) {
            match.setPlayer3(userRepository.findUserById(dto.getDifficulty() + 1L));
            newMatchPlayer.setSlot(3);
        } else if (match.getPlayer4() == null) {
            match.setPlayer4(userRepository.findUserById(dto.getDifficulty() + 1L));
            newMatchPlayer.setSlot(4);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No more players can be added");
        }

        List<MatchPlayer> matchPlayers = match.getMatchPlayers();
        matchPlayers.add(newMatchPlayer);
        match.setMatchPlayers(matchPlayers);

        aiPlayers.add(dto.getDifficulty());
        match.setAiPlayers(aiPlayers);

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

        MatchPlayer userToMatchPlayer = matchPlayerRepository.findMatchPlayerByUser(user);

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

        match.getJoinRequests().put(userId, "accepted");

        User user = userRepository.findUserById(userId);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        MatchPlayer newMatchPlayer = new MatchPlayer();
        newMatchPlayer.setPlayerId(user);
        newMatchPlayer.setMatch(match);

        match.getMatchPlayers().add(newMatchPlayer);

        if (match.getPlayer2() == null) {
            match.setPlayer2(user);
        } else if (match.getPlayer3() == null) {
            match.setPlayer3(user);
        } else if (match.getPlayer4() == null) {
            match.setPlayer4(user);
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
}
