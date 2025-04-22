package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePassingDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayedCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerMatchInformationDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Game Controller
 * This class is responsible for all requests regarding games, e.g. play a card,
 * specific match information (what cards do I have, whose turn is it, ...),
 * etc.
 * The controller will receive the request and delegate the execution to the
 * GameService and finally return the result.
 */
@RestController
public class GameController {
    private final GameService gameService;

    GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Gets only the information necessary for the player requesting the
     * information.
     * 
     * @return The information of the match
     */
    @PostMapping("/matches/{matchId}/logic")
    @ResponseStatus(HttpStatus.OK)
    public PlayerMatchInformationDTO getPlayerMatchInformation(@PathVariable Long matchId,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return gameService.getPlayerMatchInformation(token, matchId);
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
        gameService.startMatch(matchId, token);
    }

    @PostMapping("/matches/{matchId}/play")
    @ResponseStatus(HttpStatus.OK)
    public void playCard(@PathVariable Long matchId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PlayedCardDTO playedCardDTO) {
        String token = authHeader.replace("Bearer ", "");
        gameService.playCard(token, matchId, playedCardDTO);
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
        gameService.makePassingHappen(matchId, passingDTO, token);
    }
}
