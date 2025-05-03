package ch.uzh.ifi.hase.soprafs24.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ch.uzh.ifi.hase.soprafs24.entity.Match;
import ch.uzh.ifi.hase.soprafs24.entity.MatchMessage;

public interface MatchMessageRepository extends JpaRepository<MatchMessage, Long> {
    List<MatchMessage> findByMatch(Match match);
}
