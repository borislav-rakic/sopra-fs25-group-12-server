package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayer;
import ch.uzh.ifi.hase.soprafs24.entity.MatchPlayerCards;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository("matchPlayerCardsRepository")
public interface MatchPlayerCardsRepository extends JpaRepository<MatchPlayerCards, Long>, JpaSpecificationExecutor<MatchPlayerCards> {
    MatchPlayerCards findMatchPlayerCardsByMatchPlayerCardsId(Long id);
}
