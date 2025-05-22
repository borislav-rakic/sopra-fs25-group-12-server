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
    boolean existsByGameAndFromMatchPlayerSlotAndRankSuit(Game game, int fromMatchPlayerSlot, String rankSuit);

    // Check if anyone passed a specific card
    boolean existsByGameAndRankSuit(Game game, String rankSuit);

    int countByGame(Game game);

    // Count cards passed by player in specific game round
    int countByGameAndFromMatchPlayerSlotAndGameNumber(Game game, int fromMatchPlayerSlot, int gameNumber);

}
