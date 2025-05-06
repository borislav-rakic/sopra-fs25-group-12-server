package ch.uzh.ifi.hase.soprafs24.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ch.uzh.ifi.hase.soprafs24.entity.MatchMessage;

public interface MatchSummaryRepository extends JpaRepository<MatchMessage, Long> {
}
