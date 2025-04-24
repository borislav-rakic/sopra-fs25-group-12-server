package ch.uzh.ifi.hase.soprafs24.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Match;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Game findFirstByMatchAndPhaseNotIn(Match match, List<GamePhase> excludedPhases);
}
