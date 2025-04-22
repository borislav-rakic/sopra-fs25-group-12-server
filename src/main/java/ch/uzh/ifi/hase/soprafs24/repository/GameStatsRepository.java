package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("gameStatsRepository")
public interface GameStatsRepository extends JpaRepository<GameStats, Long> {
    List<GameStats> findByMatch(Match match);

    GameStats findByRankSuit(String rankSuit);

    void deleteByMatch(Match match);

    GameStats findByRankSuitAndGameAndCardHolder(String rankSuit, Game game, int cardHolder);

    List<GameStats> findByGameAndCardHolder(Game game, int cardHolder);

}
