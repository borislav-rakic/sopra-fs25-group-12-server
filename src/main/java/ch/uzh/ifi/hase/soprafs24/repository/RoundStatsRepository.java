package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.RoundStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("roundStatsRepository")
public interface RoundStatsRepository extends JpaRepository<RoundStats, Long> {
    List<RoundStats> findByMatch(Match match);

    void deleteByMatch(Match match);

}
