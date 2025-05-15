package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Match;

import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository("matchRepository")
public interface MatchRepository extends JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {
  Match findMatchByMatchId(Long matchId);

  Match findByMatchPlayersUserId(Long userId);

  @Query(value = "SELECT * FROM match WHERE host_id = :hostId AND phase NOT IN ('FINISHED', 'ABORTED') ORDER BY match_id ASC", nativeQuery = true)
  List<Match> findActiveMatchesByHostId(@Param("hostId") Long hostId);

  @Query("""
          SELECT m FROM Match m
          WHERE (m.player1.id = :userId OR m.player2.id = :userId OR m.player3.id = :userId OR m.player4.id = :userId)
            AND m.phase IN ('READY', 'IN_PROGRESS', 'BETWEEN_GAMES', 'BEFORE_GAMES')
      """)
  List<Match> findActiveMatchesWithUserId(@Param("userId") Long userId);

  @Query("""
          SELECT m.matchId FROM Match m
          WHERE (m.player1.id = :userId OR m.player2.id = :userId OR m.player3.id = :userId OR m.player4.id = :userId)
            AND m.phase IN ('SETUP')
      """)
  List<Long> findMatchIdsInSetupWithUserId(@Param("userId") Long userId);

  @Query("""
          SELECT m.matchId FROM Match m
          WHERE (m.player1.id = :userId OR m.player2.id = :userId OR m.player3.id = :userId OR m.player4.id = :userId)
            AND m.phase IN ('READY', 'IN_PROGRESS', 'BETWEEN_GAMES')
      """)
  List<Long> findActiveMatchIdsWithUserId(@Param("userId") Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT m FROM Match m LEFT JOIN FETCH m.games WHERE m.id = :id")
  Match findMatchForUpdate(@Param("id") Long id);
}
