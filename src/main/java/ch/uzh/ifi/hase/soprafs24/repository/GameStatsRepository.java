package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameStats;
import ch.uzh.ifi.hase.soprafs24.entity.Match; // <-- ADD THIS IMPORT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("gameStatsRepository")
public interface GameStatsRepository extends JpaRepository<GameStats, Long> {

    List<GameStats> findByMatch(Match match);

    GameStats findByRankSuit(String rankSuit);

    void deleteByMatch(Match match);

    GameStats findByRankSuitAndGameAndCardHolder(String rankSuit, Game game, int cardHolder);

    List<GameStats> findByGameAndCardHolder(Game game, int cardHolder);

    GameStats findByRankAndSuitAndGame(Rank rank, Suit suit, Game game);

}
