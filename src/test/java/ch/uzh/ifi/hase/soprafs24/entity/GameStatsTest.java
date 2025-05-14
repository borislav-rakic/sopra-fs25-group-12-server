package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.Rank;
import ch.uzh.ifi.hase.soprafs24.constant.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameStatsTest {

    private GameStats stats;

    @BeforeEach
    void setup() {
        stats = new GameStats();
    }

    @Test
    void testSetCardFromString_valid() {
        stats.setCardFromString("QH");
        assertEquals(Rank.Q, stats.getRank());
        assertEquals(Suit.H, stats.getSuit());
        assertEquals("QH", stats.getRankSuit());
    }

    @Test
    void testSetCardFromString_invalidFormat_throws() {
        assertThrows(IllegalArgumentException.class, () -> stats.setCardFromString("ZZ"));
        assertThrows(IllegalArgumentException.class, () -> stats.setCardFromString(""));
        assertThrows(IllegalArgumentException.class, () -> stats.setCardFromString("2"));
    }

    @Test
    void testSetRankAndSuit_updatesRankSuit() {
        stats.setRank(Rank.A);
        stats.setSuit(Suit.S);
        assertEquals("AS", stats.getRankSuit());
    }

    @Test
    void testPossibleHolders_bitOperations() {
        stats.setPossibleHolder(1);
        stats.setPossibleHolder(3);

        assertTrue(stats.isPossibleHolder(1));
        assertFalse(stats.isPossibleHolder(2));
        assertTrue(stats.isPossibleHolder(3));
        assertFalse(stats.isPossibleHolder(4));

        stats.clearPossibleHolder(3);
        assertFalse(stats.isPossibleHolder(3));

        stats.setOnlyPossibleHolder(2);
        assertTrue(stats.isPossibleHolder(2));
        assertEquals(List.of(2), stats.getPossibleHolderList());
    }

    @Test
    void testSetOnlyPossibleHolder_validatesSlot() {
        assertThrows(IllegalArgumentException.class, () -> stats.setOnlyPossibleHolder(0));
        assertThrows(IllegalArgumentException.class, () -> stats.setOnlyPossibleHolder(5));
    }

    @Test
    void testIsPlayed_logic() {
        stats.setPlayOrder(0);
        assertFalse(stats.isPlayed());

        stats.setPlayOrder(1);
        assertTrue(stats.isPlayed());
    }
}
