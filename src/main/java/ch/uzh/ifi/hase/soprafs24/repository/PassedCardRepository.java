package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.PassedCard;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassedCardRepository extends JpaRepository<PassedCard, Long> {

    List<PassedCard> findByGame(Game game);

    List<PassedCard> findByGameAndFromPlayer(Game game, User fromPlayer);

    boolean existsByGameAndRankSuit(Game game, String rankSuit);

    long countByGame(Game game);
}
