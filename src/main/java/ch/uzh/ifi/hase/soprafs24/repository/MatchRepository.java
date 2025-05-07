package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Match;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository("matchRepository")
public interface MatchRepository extends JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {
    Match findMatchByMatchId(Long matchId);

    Match findByMatchPlayersUserId(Long userId);

    @Query(value = "SELECT * FROM match WHERE host_id = :hostId AND phase NOT IN ('FINISHED', 'ABORTED') ORDER BY match_id ASC", nativeQuery = true)
    List<Match> findActiveMatchesByHostId(@Param("hostId") Long hostId);
}
