package ch.uzh.ifi.hase.soprafs24.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ch.uzh.ifi.hase.soprafs24.entity.Game;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Game findGameByMatch_MatchId(Long matchId);
}
