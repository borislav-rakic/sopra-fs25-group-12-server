package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository("matchPlayerRepository")
public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, Long>, JpaSpecificationExecutor<MatchPlayer> {
    MatchPlayer findByUserAndMatch(User user, Match match);
    MatchPlayer findByUserAndMatchAndSlot(User user, Match match, int slot);
    MatchPlayer findByMatchAndSlot(Match match, int slot);
}
