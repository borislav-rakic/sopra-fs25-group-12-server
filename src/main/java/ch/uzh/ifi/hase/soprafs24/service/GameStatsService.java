package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
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
public class GameStatsService {

    private final Logger log = LoggerFactory.getLogger(GameStatsService.class);

    private final GameStatsRepository gameStatsRepository;
    private final MatchRepository matchRepository;

    @Autowired
    public GameStatsService(
            @Qualifier("gameStatsRepository") GameStatsRepository gameStatsRepository,
            @Qualifier("matchRepository") MatchRepository matchRepository) {
        this.gameStatsRepository = gameStatsRepository;
        this.matchRepository = matchRepository;
    }

    public void initializeGameStats(Match match) {
        List<GameStats> gameStatsList = new ArrayList<>();

        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                GameStats stats = new GameStats();
                stats.setMatch(match);
                stats.setSuit(suit);
                stats.setRank(rank);
                stats.setPlayOrder(0); // Not yet played
                stats.setPlayedBy(0); // Not yet played
                stats.setAllowedToPlay(false);
                stats.setPossibleHolders(0b1111); // All 4 players (bitmask)
                stats.setPointsBilledTo(0);
                stats.setCardHolder(0);

                gameStatsList.add(stats);
            }
        }

        gameStatsRepository.saveAll(gameStatsList);
        gameStatsRepository.flush();

        log.info("Initialized 52 game stats entries for Match ID: {}", match.getMatchId());
    }

    public List<GameStats> getGameStatsForMatch(Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);

        if (match == null) {
            throw new IllegalArgumentException("Match not found with ID: " + matchId);
        }

        return gameStatsRepository.findByMatch(match);
    }

    public void deleteGameStatsForMatch(Match match) {
        gameStatsRepository.deleteByMatch(match);
        log.info("Deleted game stats for Match ID: {}", match.getMatchId());
    }
}
