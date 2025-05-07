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

    void deleteByMatch(Match match);

    GameStats findByRankSuitAndGameAndCardHolder(String rankSuit, Game game, int cardHolder);

    GameStats findByRankAndSuitAndGame(Rank rank, Suit suit, Game game);

    GameStats findByRankSuitAndGame(String rankSuit, Game game);

    GameStats findByGameAndRankSuit(Game game, String rankSuit);

    long countByGameAndPlayedByGreaterThan(Game game, int threshold);

    List<GameStats> findByGameAndPlayOrderGreaterThanOrderByPlayOrderAsc(Game game, int playOrder);

    List<GameStats> findByGameAndPlayedByGreaterThan(Game game, int playedBy);

    List<GameStats> findByGameAndTrickNumber(Game game, int trickNumber);

    List<GameStats> findByGame(Game game);

    List<GameStats> findByGameAndPointsBilledTo(Game game, int pointsBilledTo);

    List<GameStats> findAllByGame(Game game);

    @Query("SELECT gs FROM GameStats gs WHERE gs.game.gameId = :gameId")
    List<GameStats> findAllByGameId(@Param("gameId") Long gameId);

}
