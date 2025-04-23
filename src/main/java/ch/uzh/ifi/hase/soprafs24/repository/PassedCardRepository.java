package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.PassedCard;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassedCardRepository extends JpaRepository<PassedCard, Long> {

    List<PassedCard> findByGame(Game game);

    // Check if a specific slot already passed a specific card
    boolean existsByGameAndFromSlotAndRankSuit(Game game, int fromSlot, String rankSuit);

    // Check if anyone passed a specific card
    boolean existsByGameAndRankSuit(Game game, String rankSuit);

    long countByGame(Game game);
}
