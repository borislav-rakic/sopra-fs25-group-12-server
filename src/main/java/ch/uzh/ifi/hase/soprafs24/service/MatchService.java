package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchCreateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MatchDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

    @Autowired
    public MatchService(@Qualifier("matchRepository") MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public Match createNewMatch(Match newMatch) {
        newMatch = matchRepository.save(newMatch);
        matchRepository.flush();

        System.out.println("MatchId: " + newMatch.getMatchId());
        System.out.println("PlayerIds: " + newMatch.getPlayerIds());

        return newMatch;
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
}
