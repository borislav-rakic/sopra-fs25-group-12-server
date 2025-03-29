package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchCreateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.MatchService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

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
        Match match = DTOMapper.INSTANCE.convertMatchCreateDTOtoEntity(matchCreateDTO);

        return DTOMapper.INSTANCE.convertEntityToMatchDTO(matchService.createNewMatch(match));
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
}
