package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository("matchRepository")
public interface MatchRepository extends JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {
    Match findMatchByMatchId(Long matchId);

    boolean existsByHostIdAndStarted(Long hostId, boolean started);

    Match findByHostIdAndStarted(Long hostId, boolean started);

    Match findByMatchPlayersUserId(Long userId);
}
