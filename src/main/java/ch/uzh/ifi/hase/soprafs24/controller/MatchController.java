package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchCreateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.AIPlayerDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.MatchService;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Match Controller
 * This class is responsible for handling all REST request that are related to
 * matches.
 * The controller will receive the request and delegate the execution to the
 * MatchService and finally return the result.
 */
@RestController
public class MatchController {
    private final MatchService matchService;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final Logger log = LoggerFactory.getLogger(MatchController.class);


    MatchController(MatchService matchService, MatchRepository matchRepository, UserRepository userRepository) {
        this.matchService = matchService;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
    }
    

    /**
     * Creates a new entry in the MATCH relation and returns the entry if it was successful.
     * @param matchCreateDTO The object that was sent by a player when starting a new match (the host's token).
     * @return The created match.
     */
    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchDTO createNewMatch(@RequestBody MatchCreateDTO matchCreateDTO) {
        System.out.println("ID: " + matchCreateDTO.getPlayerToken());

        return DTOMapper.INSTANCE.convertEntityToMatchDTO(matchService.createNewMatch(matchCreateDTO));
    }

    /**
     * Gets the information for all active matches in the database.
     * @return The information of the matches.
     */
    @GetMapping("/matches")
    @ResponseStatus(HttpStatus.OK)
    public List<MatchDTO> getMatchesInformation() {
        List<Match> matches = matchService.getMatchesInformation();
        List<MatchDTO> matchDTOs = new ArrayList<>();

        for (Match match : matches) {
            matchDTOs.add(DTOMapper.INSTANCE.convertEntityToMatchDTO(match));
        }

        return matchDTOs;
    }

    /**
     * Gets the information for one specific match in the database.
     * @return The information of the match.
     */
    @GetMapping("/matches/{matchId}")
    @ResponseStatus(HttpStatus.OK)
    public MatchDTO getMatchInformation(@PathVariable Long matchId) {
        return DTOMapper.INSTANCE.convertEntityToMatchDTO(matchService.getMatchInformation(matchId));
    }

    /**
     * Deletes the information for one specific match in the database.
     */
    @DeleteMapping("/matches/{matchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMatch(
            @PathVariable Long matchId,
            @RequestHeader("Authorization") String authHeader) {
        // remove "Bearer " prefix from token
        String token = authHeader.replace("Bearer ", "");
        matchService.deleteMatchByHost(matchId, token);
    }

    @PostMapping("/matches/{matchId}/invite")
    @ResponseStatus(HttpStatus.OK)
    public void invitePlayerToMatch(
        @PathVariable Long matchId,
        @RequestBody InviteRequestDTO request
    ) {
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



    @PostMapping("/matches/{matchId}/invite/respond")
    @ResponseStatus(HttpStatus.OK)
    public void respondToInvite(
            @PathVariable Long matchId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody InviteResponseDTO responseDTO
    ) {

        if (responseDTO == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is missing or invalid");
    }

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

    @PostMapping("/matches/{matchId}/length")
    @ResponseStatus(HttpStatus.OK)
    public void updateMatchLength(
        @PathVariable Long matchId,
        @RequestBody Map<String, Integer> body
    ) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));

        if (!body.containsKey("length")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'length' field");
        }

        match.setLength(body.get("length"));
        matchRepository.save(match);
    }

    @PostMapping("/matches/{matchId}/ai")
    @ResponseStatus(HttpStatus.OK)
    public void addAiPlayer(
        @PathVariable Long matchId,
        @RequestBody AIPlayerDTO dto
    ) {
        Match match = matchRepository.findById(matchId).orElseThrow();

        List<Integer> aiPlayers = match.getAiPlayers();
        if (aiPlayers == null) {
            aiPlayers = new ArrayList<>();
        }

        aiPlayers.add(dto.getDifficulty());
        match.setAiPlayers(aiPlayers);

        matchRepository.save(match);
    }

}
