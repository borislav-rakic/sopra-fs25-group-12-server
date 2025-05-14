package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MatchTest {

    private Match match;

    @BeforeEach
    void setUp() {
        match = new Match();
    }

    @Test
    void testSetAndGetHostInfo() {
        match.setHostId(42L);
        match.setHostUsername("testHost");

        assertEquals(42L, match.getHostId());
        assertEquals("testHost", match.getHostUsername());
    }

    @Test
    void testMatchScoresListParsing() {
        match.setMatchScoresList(List.of(4, 5, 3, 13));
        List<Integer> scores = match.getMatchScoresList();

        assertEquals(4, scores.size());
        assertEquals(13, scores.get(3));
    }

    @Test
    void testGetScoreForSlot_valid() {
        match.setMatchScoresList(List.of(4, 5, 3, 13));
        assertEquals(5, match.getScoreForSlot(2));
    }

    @Test
    void testGetScoreForSlot_invalid() {
        match.setMatchScoresList(List.of(4, 5, 3, 13));
        assertThrows(IllegalArgumentException.class, () -> match.getScoreForSlot(5));
    }

    @Test
    void testMatchPlayerNamesParsing() {
        match.setMatchPlayerNames(List.of("Alice", "Bob", "Charlie", "Diana"));
        List<String> names = match.getMatchPlayerNames();

        assertEquals(4, names.size());
        assertEquals("Charlie", names.get(2));
    }

    @Test
    void testGetNameForSlot_valid() {
        match.setMatchPlayerNames(List.of("A", "B", "C", "D"));
        assertEquals("C", match.getNameForSlot(3));
    }

    @Test
    void testGetNameForSlot_invalid() {
        match.setMatchPlayerNames(List.of("A", "B", "C", "D"));
        assertThrows(IllegalArgumentException.class, () -> match.getNameForSlot(5));
    }

    @Test
    void testAddSlotDidConfirm() {
        match.addSlotDidConfirm(1);
        match.addSlotDidConfirm(3);
        match.addSlotDidConfirm(1); // should not duplicate

        List<Integer> confirmed = match.getSlotDidConfirmLastGame();
        assertEquals(2, confirmed.size());
        assertTrue(confirmed.contains(1));
        assertTrue(confirmed.contains(3));
    }

    @Test
    void testGetAndSetPhase() {
        match.setPhase(MatchPhase.SETUP);
        assertEquals(MatchPhase.SETUP, match.getPhase());
    }

    @Test
    void testFastForwardModeToggle() {
        assertFalse(match.getFastForwardMode());
        match.setFastForwardMode(true);
        assertTrue(match.getFastForwardMode());
    }

    @Test
    void testJoinRequestStatus_default() {
        assertEquals("not found", match.getJoinRequestStatus(99L));
    }

    @Test
    void testJoinRequestStatus_withEntry() {
        match.setJoinRequests(Map.of(42L, "pending"));
        assertEquals("pending", match.getJoinRequestStatus(42L));
    }
}
