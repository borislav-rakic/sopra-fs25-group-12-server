package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchStats;
import ch.uzh.ifi.hase.soprafs24.entity.RoundStats;
import ch.uzh.ifi.hase.soprafs24.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("matchStatsRepository")
public interface MatchStatsRepository extends JpaRepository<MatchStats, Long> {
    List<MatchStats> findByMatch(Match match);

    MatchStats findByMatchAndPlayer(Match match, User player);
}
