package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchCreateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.MatchService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

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

    MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    /**
     * Creates a new entry in the MATCH relation and returns the entry if it was successful.
     * @param matchCreateDTO The object that was sent by a player when starting a new match (a list with 4
     * elements, where the host's id is the first element).
     * @return The created match.
     */
    @PostMapping("/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchDTO createNewMatch(@RequestBody MatchCreateDTO matchCreateDTO) {
        System.out.println(matchCreateDTO.getPlayerIds().get(0));
        Match match = DTOMapper.INSTANCE.convertMatchCreateDTOtoEntity(matchCreateDTO);

        return DTOMapper.INSTANCE.convertEntityToMatchDTO(matchService.createNewMatch(match));
    }
}
