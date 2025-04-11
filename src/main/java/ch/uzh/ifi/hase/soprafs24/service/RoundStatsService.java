package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.RoundStats;
import ch.uzh.ifi.hase.soprafs24.entity.RoundStats.Rank;
import ch.uzh.ifi.hase.soprafs24.entity.RoundStats.Suit;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.RoundStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class RoundStatsService {

    private final Logger log = LoggerFactory.getLogger(RoundStatsService.class);

    private final RoundStatsRepository roundStatsRepository;
    private final MatchRepository matchRepository;

    @Autowired
    public RoundStatsService(
            @Qualifier("roundStatsRepository") RoundStatsRepository roundStatsRepository,
            @Qualifier("matchRepository") MatchRepository matchRepository) {
        this.roundStatsRepository = roundStatsRepository;
        this.matchRepository = matchRepository;
    }

    public void initializeRoundStats(Match match) {
        List<RoundStats> roundStatsList = new ArrayList<>();

        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                RoundStats stats = new RoundStats();
                stats.setMatch(match);
                stats.setSuit(suit);
                stats.setRank(rank);
                stats.setPlayOrder(0); // Not yet played
                stats.setPlayedBy(0); // Not yet played
                stats.setAllowedToPlay(false);
                stats.setPossibleHolders(0b1111); // All 4 players (bitmask)
                stats.setPointsBilledTo(0);
                stats.setCardHolder(0);

                roundStatsList.add(stats);
            }
        }

        roundStatsRepository.saveAll(roundStatsList);
        roundStatsRepository.flush();

        log.info("Initialized 52 round stats entries for Match ID: {}", match.getMatchId());
    }

    public List<RoundStats> getRoundStatsForMatch(Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);

        if (match == null) {
            throw new IllegalArgumentException("Match not found with ID: " + matchId);
        }

        return roundStatsRepository.findByMatch(match);
    }

    public void deleteRoundStatsForMatch(Match match) {
        roundStatsRepository.deleteByMatch(match);
        log.info("Deleted round stats for Match ID: {}", match.getMatchId());
    }
}
