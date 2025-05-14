package ch.uzh.ifi.hase.soprafs24.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchSummaryTest {

    @Test
    void testSettersAndGetters() {
        MatchSummary summary = new MatchSummary();

        Long id = 42L;
        String matchHtml = "<p>Match summary content</p>";
        String gameHtml = "<p>Game summary content</p>";
        Match match = new Match();

        summary.setId(id);
        summary.setMatchSummaryHtml(matchHtml);
        summary.setGameSummaryHtml(gameHtml);
        summary.setMatch(match);

        assertEquals(id, summary.getId());
        assertEquals(matchHtml, summary.getMatchSummaryHtml());
        assertEquals(gameHtml, summary.getGameSummaryHtml());
        assertEquals(match, summary.getMatch());
    }
}
