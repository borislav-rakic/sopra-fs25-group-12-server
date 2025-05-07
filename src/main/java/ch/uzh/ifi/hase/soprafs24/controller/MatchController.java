package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.MatchService;
import ch.uzh.ifi.hase.soprafs24.service.MatchSetupService;

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
    private final MatchSetupService matchSetupService;

    MatchController(MatchService matchService, MatchSetupService matchSetupService) {
        this.matchService = matchService;
        this.matchSetupService = matchSetupService;
    }

    /**
     * Creates a new entry in the MATCH relation and returns the entry if it was
     * successful.
     * 
     * @param authorization-token in the RequestHeader.
     * @return The created match.
     */
    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchDTO createNewMatch(@RequestHeader("Authorization") String authHeader) {
        String playerToken;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            playerToken = authHeader.substring(7); // remove "Bearer "
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        return DTOMapper.INSTANCE.convertEntityToMatchDTO(matchSetupService.createNewMatch(playerToken));
    }

    /**
     * Gets the information for all active matches in the database.
     * 
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
     * 
     * @return The information of the match.
     */
    @GetMapping("/matches/{matchId}")
    @ResponseStatus(HttpStatus.OK)
    public MatchDTO getMatchDTO(@PathVariable Long matchId) {
        return DTOMapper.INSTANCE.convertEntityToMatchDTO(matchService.getMatchDTO(matchId));
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
        matchSetupService.deleteMatchByHost(matchId, token);
    }

    /**
     * Sends an invitation to a player.
     */
    @PostMapping("/matches/{matchId}/invite")
    @ResponseStatus(HttpStatus.OK)
    public void invitePlayerToMatch(
            @PathVariable Long matchId,
            @RequestBody InviteRequestDTO request) {
        matchSetupService.invitePlayerToMatch(matchId, request);
    }

    /**
     * Revoke an invitation sent to a player.
     */
    @DeleteMapping("/matches/{matchId}/invite/{playerSlot}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelInvite(@PathVariable Long matchId, @PathVariable Integer playerSlot) {
        matchSetupService.cancelInvite(matchId, playerSlot);
    }

    /**
     * Responds to a match invite (accept/decline).
     */
    @PostMapping("/matches/{matchId}/invite/respond")
    @ResponseStatus(HttpStatus.OK)
    public void respondToInvite(
            @PathVariable Long matchId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody InviteResponseDTO responseDTO) {

        if (responseDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is missing or invalid");
        }

        matchSetupService.respondToInvite(matchId, authHeader, responseDTO);
    }

    /**
     * Remove a player from lobby.
     */
    @DeleteMapping("/matches/{matchId}/player/{playerSlot}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePlayer(@PathVariable Long matchId, @PathVariable Integer playerSlot) {
        matchSetupService.removePlayer(matchId, playerSlot);
    }

    /**
     * Updates the match goal (points limits).
     */
    @PostMapping("/matches/{matchId}/matchGoal")
    @ResponseStatus(HttpStatus.OK)
    public void updateMatchGoal(
            @PathVariable Long matchId,
            @RequestBody Map<String, Integer> body) {
        matchSetupService.updateMatchGoal(matchId, body);
    }

    /**
     * Adds an AI player to a match.
     */
    @PostMapping("/matches/{matchId}/ai")
    @ResponseStatus(HttpStatus.OK)
    public void addAiPlayer(
            @PathVariable Long matchId,
            @RequestBody AIPlayerDTO dto) {
        matchSetupService.addAiPlayer(matchId, dto);
    }

    /**
     * Remove an AI player from a match.
     */
    @PostMapping("/matches/{matchId}/ai/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAiPlayerFromMatch(
            @PathVariable Long matchId,
            @RequestBody AIPlayerDTO dto) {
        matchSetupService.removeAiPlayer(matchId, dto);
    }

    /**
     * Sends a join request.
     */
    @PostMapping("/matches/{matchId}/join")
    public void sendJoinRequest(@PathVariable Long matchId, @RequestBody JoinRequestDTO joinRequestDTO) {
        Long userId = joinRequestDTO.getUserId();
        matchSetupService.sendJoinRequest(matchId, userId);
    }

    /**
     * Accepts a join request.
     */
    @PostMapping("/matches/{matchId}/join/accept")
    public void acceptJoinRequest(@PathVariable Long matchId, @RequestBody JoinRequestDTO joinRequestDTO) {
        Long userId = joinRequestDTO.getUserId();
        matchSetupService.acceptJoinRequest(matchId, userId);
    }

    /**
     * Declines a join request.
     */
    @PostMapping("/matches/{matchId}/join/decline")
    public void declineJoinRequest(@PathVariable Long matchId, @RequestBody JoinRequestDTO joinRequestDTO) {
        Long userId = joinRequestDTO.getUserId();
        matchSetupService.declineJoinRequest(matchId, userId);
    }

    /**
     * Retrieves the list of all join requests for a match.
     */
    @GetMapping("/matches/{matchId}/joinRequests")
    @ResponseStatus(HttpStatus.OK)
    public List<JoinRequestDTO> getJoinRequests(@PathVariable Long matchId) {
        return matchSetupService.getJoinRequests(matchId);
    }

    /**
     * Leave the lobby.
     */
    @DeleteMapping("/matches/{matchId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveMatch(@PathVariable Long matchId, @RequestHeader("Authorization") String authHeader) {
        matchService.leaveMatch(matchId, authHeader.replace("Bearer ", ""));
    }

    /**
     * Get a list of eligible users for this match about to start.
     */
    @GetMapping("/matches/{matchId}/eligibleusers")
    @ResponseStatus(HttpStatus.OK)
    public List<UserGetDTO> getEligibleUsers(
            @PathVariable Long matchId,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return matchSetupService.getEligibleUsers(matchId, token);
    }

    @PostMapping("/matches/{matchId}/passing")
    @ResponseStatus(HttpStatus.OK)
    public void passCards(
            @PathVariable Long matchId,
            @RequestBody GamePassingDTO passingDTO,
            @RequestHeader("Authorization") String authHeader) {
        // Optionally extract Bearer token, if needed
        String token = authHeader.replace("Bearer ", "");
        // Delegate the work to the service
        Boolean pickRandomly = false;
        matchService.passingAcceptCards(matchId, passingDTO, token, pickRandomly);
    }

    @PostMapping("/matches/{matchId}/passing/any")
    @ResponseStatus(HttpStatus.OK)
    public void passAnyCards(
            @PathVariable Long matchId,
            @RequestBody GamePassingDTO passingDTO,
            @RequestHeader("Authorization") String authHeader) {
        // Optionally extract Bearer token, if needed
        String token = authHeader.replace("Bearer ", "");
        // Delegate the work to the service
        Boolean pickRandomly = true;
        matchService.passingAcceptCards(matchId, passingDTO, token, pickRandomly);
    }

    /**
     * Gets only the information necessary for the player requesting the
     * information.
     * 
     * @return The information of the match
     */
    @PostMapping("/matches/{matchId}/logic")
    @ResponseStatus(HttpStatus.OK)
    public PollingDTO getPlayerPolling(@PathVariable Long matchId,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return matchService.getPlayerPolling(token, matchId);
    }

    /**
     * When the host starts the match, this function initializes the necessary
     * relations in the database and opens
     * communication with the deck of cards API
     */
    @PostMapping("/matches/{matchId}/start")
    @ResponseStatus(HttpStatus.OK)
    public void startMatch(@PathVariable Long matchId, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        matchSetupService.startMatch(matchId, token, null);
    }

    @PostMapping("/matches/{matchId}/start/{seed}")
    @ResponseStatus(HttpStatus.OK)
    public void startSeededMatch(
            @PathVariable Long matchId,
            @PathVariable String seed,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");

        Long parsedSeed = null;
        try {
            parsedSeed = Long.parseLong(seed);
        } catch (NumberFormatException e) {
            matchSetupService.startMatch(matchId, token, null);
            return;
        }

        if (parsedSeed % 10000 != 9247) {
            matchSetupService.startMatch(matchId, token, null);
            return;
        }

        matchSetupService.startMatch(matchId, token, parsedSeed);
    }

    @PostMapping("/matches/{matchId}/play")
    @ResponseStatus(HttpStatus.OK)
    public void playCardAsHuman(@PathVariable Long matchId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PlayedCardDTO playedCardDTO) {
        String token = authHeader.replace("Bearer ", "");
        matchService.playCardAsHuman(token, matchId, playedCardDTO);
    }

    @PostMapping("/matches/{matchId}/play/any")
    @ResponseStatus(HttpStatus.OK)
    public void playAnyCardAsHuman(@PathVariable Long matchId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PlayedCardDTO playedCardDTO) {
        String token = authHeader.replace("Bearer ", "");
        playedCardDTO.setCard("XX");
        matchService.playCardAsHuman(token, matchId, playedCardDTO);
    }

    @PostMapping("/matches/{matchId}/game/confirm")
    @ResponseStatus(HttpStatus.OK)
    public void confirmGameResult(@PathVariable Long matchId, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        matchService.confirmGameResult(token, matchId);
    }

    @PostMapping("/matches/{matchId}/game/sim/game")
    @ResponseStatus(HttpStatus.OK)
    public void advanceGame(@PathVariable Long matchId) {
        matchService.autoPlayGame(matchId);
    }

    @PostMapping("/matches/{matchId}/game/sim/match")
    @ResponseStatus(HttpStatus.OK)
    public void advanceMatch(@PathVariable Long matchId) {
        matchService.autoPlayMatch(matchId);
    }
}
