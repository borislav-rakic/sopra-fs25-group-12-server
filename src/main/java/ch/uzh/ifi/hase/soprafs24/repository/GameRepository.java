package ch.uzh.ifi.hase.soprafs24.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs24.entity.Game;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Game findGameByGameId(Long gameId);

    // Find game after async call to external API.
    @Query("""
                SELECT g FROM Game g
                WHERE g.match.id = :matchId
                AND g.phase IN ('WAITING_FOR_EXTERNAL_API')
                ORDER BY g.gameNumber DESC
            """)
    List<Game> findWaitingGameByMatchid(Long matchId);

    // Find only current active game per match.
    @Query("""
                SELECT g FROM Game g
                WHERE g.match.id = :matchId
                AND g.phase NOT IN ('FINISHED', 'ABORTED')
                ORDER BY g.gameNumber DESC
            """)
    Game findActiveGameByMatchId(@Param("matchId") Long matchId);
}
