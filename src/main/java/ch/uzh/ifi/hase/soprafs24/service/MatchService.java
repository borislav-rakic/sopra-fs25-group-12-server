package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.AIPlayerDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchCreateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final MatchRepository matchRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    @Autowired
    public MatchService(@Qualifier("matchRepository") MatchRepository matchRepository, UserService userService, @Qualifier("userRepository") UserRepository userRepository) {
        this.matchRepository = matchRepository;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    public Match createNewMatch(MatchCreateDTO newMatch) {
        User user = userService.getUserByToken(newMatch.getPlayerToken());

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        Match match = new Match();

        List<Long> playerList = new ArrayList<>();
        playerList.add(user.getId());
        playerList.add(null);
        playerList.add(null);
        playerList.add(null);

        match.setPlayerIds(playerList);
        match.setHost(user.getUsername());
        match.setLength(100);
        match.setStarted(false);

        matchRepository.save(match);
        matchRepository.flush();

        System.out.println(match.getMatchId());

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

        return matchRepository.findMatchByMatchId(matchId);
    }

    public void deleteMatchByHost(Long matchId, String token) {
        Match match = matchRepository.findMatchByMatchId(matchId);
    
        if (match == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found");
        }
        
        matchRepository.delete(match); 
    }

    public void invitePlayerToMatch(Long matchId, InviteRequestDTO request) {
        Long userId = request.getUserId();
        Integer playerSlot = request.getPlayerSlot();

        Match match = matchRepository.findById(matchId).orElseThrow();

        Map<Integer, Long> invites = match.getInvites();
        if (invites == null) {
            invites = new java.util.HashMap<>();
        }

        invites.put(playerSlot, userId);
        match.setInvites(invites);

        matchRepository.save(match);
    }

    public void respondToInvite(Long matchId, String authHeader, InviteResponseDTO responseDTO) {
        String token = authHeader.replace("Bearer ", "");
        User user = userRepository.findUserByToken(token);

        Match match = matchRepository.findById(matchId).orElse(null);

        Map<Integer, Long> invites = match.getInvites();

        Integer slot = null;

        for (Map.Entry<Integer, Long> entry : invites.entrySet()) {
            if (entry.getValue().equals(user.getId())) {
                slot = entry.getKey();
                break;
            }
        }

        if (responseDTO.isAccepted()) {
            List<Long> players = match.getPlayerIds();
            while (players.size() <= slot) {
                players.add(null);
            }
            players.set(slot, user.getId());
            match.setPlayerIds(players);
        }
        invites.remove(slot);
        match.setInvites(invites);

        matchRepository.save(match);
    }

    public void updateMatchLength(Long matchId, Map<String, Integer> body) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));

        if (!body.containsKey("length")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'length' field");
        }

        match.setLength(body.get("length"));
        matchRepository.save(match);
    }

    public void addAiPlayer(Long matchId, AIPlayerDTO dto) {
        Match match = matchRepository.findById(matchId).orElseThrow();

        List<Integer> aiPlayers = match.getAiPlayers();
        if (aiPlayers == null) {
            aiPlayers = new ArrayList<>();
        }

        aiPlayers.add(dto.getDifficulty());
        match.setAiPlayers(aiPlayers);

        matchRepository.save(match);
    }

    public Match gameLogic(MatchCreateDTO matchCreateDTO, Long matchId) {
        return new Match();
    }
}

