package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.repository.MatchRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public void initializeGameStats(Match match, Game game) {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                GameStats gameStats = new GameStats();
                gameStats.setMatch(match);
                gameStats.setGame(game);
                gameStats.setSuit(suit);
                gameStats.setRank(rank);
                gameStats.setPlayOrder(0);
                gameStats.setPlayedBy(0);
                gameStats.setPointsWorth(
                        suit == Suit.S && rank == Rank.Q ? 13 : (suit == Suit.H ? 1 : 0));
                gameStats.setPossibleHolders(0b1111);
                gameStats.setPointsBilledTo(0);
                gameStats.setCardHolder(0); // to be filled right after
                gameStatsRepository.save(gameStats);
            }
        }
        gameStatsRepository.flush();
    }

    public List<GameStats> getGameStatsForMatch(Long matchId) {
        Match match = matchRepository.findMatchByMatchId(matchId);

        if (match == null) {
            throw new IllegalArgumentException("Match not found with ID: " + matchId);
        }

        List<GameStats> stats = gameStatsRepository.findByMatch(match);

        if (stats == null || stats.isEmpty()) {
            // You can also use HttpStatus.NOT_FOUND if this is an API layer
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No game stats found for this match");
        }

        return stats;
    }

    public void deleteGameStatsForMatch(Match match) {
        gameStatsRepository.deleteByMatch(match);
        log.info("Deleted game stats for Match ID: {}", match.getMatchId());
    }

    public void setPossibleHolder(GameStats stats, int slotNumber) {
        validateSlotNumber(slotNumber);
        int mask = 1 << (slotNumber - 1);
        stats.setPossibleHolders(stats.getPossibleHolders() | mask);
    }

    public void clearPossibleHolder(GameStats stats, int slotNumber) {
        validateSlotNumber(slotNumber);
        int mask = ~(1 << (slotNumber - 1));
        stats.setPossibleHolders(stats.getPossibleHolders() & mask);
    }

    public boolean isPossibleHolder(GameStats stats, int slotNumber) {
        validateSlotNumber(slotNumber);
        int mask = 1 << (slotNumber - 1);
        return (stats.getPossibleHolders() & mask) != 0;
    }

    public List<Integer> getPossibleHolderList(GameStats stats) {
        List<Integer> holders = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            if (isPossibleHolder(stats, i)) {
                holders.add(i);
            }
        }
        return holders;
    }

    private void validateSlotNumber(int slotNumber) {
        if (slotNumber < 1 || slotNumber > 4) {
            throw new IllegalArgumentException("Slot number must be between 1 and 4");
        }
    }

}
