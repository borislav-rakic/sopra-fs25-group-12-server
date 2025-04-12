package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.MatchService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
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
//    private final Logger log = LoggerFactory.getLogger(MatchController.class);


    MatchController(MatchService matchService) {
        this.matchService = matchService;
    }
    

    /**
     * Creates a new entry in the MATCH relation and returns the entry if it was successful.
     * @param matchCreateDTO The object that was sent by a player when starting a new match (the host's token).
     * @return The created match.
     */
    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchDTO createNewMatch(@RequestBody MatchCreateDTO matchCreateDTO, @RequestHeader("Authorization") String authHeader) {
        System.out.println("TOKEN: " + matchCreateDTO.getPlayerToken());
        System.out.println("TOKEN_HEADER: " + authHeader);
        String playerToken;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            playerToken = authHeader.substring(7); // remove "Bearer "
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        return DTOMapper.INSTANCE.convertEntityToMatchDTO(matchService.createNewMatch(playerToken));
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

    /**
     * Sends an invitation to a player.
     */
    @PostMapping("/matches/{matchId}/invite")
    @ResponseStatus(HttpStatus.OK)
    public void invitePlayerToMatch(
        @PathVariable Long matchId,
        @RequestBody InviteRequestDTO request
    ) {
        matchService.invitePlayerToMatch(matchId, request);
    }

    /**
     * Responds to a match invite (accept/decline).
     */
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

        matchService.respondToInvite(matchId, authHeader, responseDTO);
    }

    /**
     * Updates the match length (points limits).
     */
    @PostMapping("/matches/{matchId}/length")
    @ResponseStatus(HttpStatus.OK)
    public void updateMatchLength(
        @PathVariable Long matchId,
        @RequestBody Map<String, Integer> body
    ) {
        matchService.updateMatchLength(matchId, body);
    }

    /**
     * Adds an AI player to a match.
     */
    @PostMapping("/matches/{matchId}/ai")
    @ResponseStatus(HttpStatus.OK)
    public void addAiPlayer(
        @PathVariable Long matchId,
        @RequestBody AIPlayerDTO dto
    ) {
        matchService.addAiPlayer(matchId, dto);
    }

    /**
     * Sends a join request.
     */
    @PostMapping("/matches/{matchId}/join")
    public void sendJoinRequest(@PathVariable Long matchId, @RequestBody JoinRequestDTO joinRequestDTO) {
        Long userId = joinRequestDTO.getUserId();
        matchService.sendJoinRequest(matchId, userId);
    }

    /**
     * Accepts a join request.
     */
    @PostMapping("/matches/{matchId}/join/accept")
    public void acceptJoinRequest(@PathVariable Long matchId, @RequestBody JoinRequestDTO joinRequestDTO) {
        Long userId = joinRequestDTO.getUserId();
        matchService.acceptJoinRequest(matchId, userId);
    }

    /**
     * Declines a join request.
     */
    @PostMapping("/matches/{matchId}/join/decline")
    public void declineJoinRequest(@PathVariable Long matchId, @RequestBody JoinRequestDTO joinRequestDTO) {
        Long userId = joinRequestDTO.getUserId();
        matchService.declineJoinRequest(matchId, userId);
    }

    /**
     * Retrieves the list of all join requests for a match.
     */
    @GetMapping("/matches/{matchId}/joinRequests")  
    public List<JoinRequestDTO> getJoinRequests(@PathVariable Long matchId) {
        return matchService.getJoinRequests(matchId);  
    }

}
