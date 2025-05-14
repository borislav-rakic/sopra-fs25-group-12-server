package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
        Match match = new Match();
        List<MatchPlayer> players = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            User user = new User();
            user.setUsername("Player" + i);
            user.setId((long) i + 1);

            MatchPlayer mp = new MatchPlayer();
            mp.setUser(user);
            mp.setMatchPlayerSlot(i + 1);
            mp.setMatch(match);
            players.add(mp);
        }

        match.setMatchPlayers(players);

        List<String> expected = List.of("Player0", "Player1", "Player2", "Player3");
        assertEquals(expected, match.getMatchPlayerNames());
    }

    @Test
    void testGetNameForMatchPlayerSlot_valid() {
        Match match = new Match();
        List<MatchPlayer> players = new ArrayList<>();

        for (int i = 1; i <= 4; i++) {
            User user = new User();
            user.setUsername(Character.toString((char) ('A' + i - 1)));

            MatchPlayer mp = new MatchPlayer();
            mp.setUser(user);
            mp.setMatchPlayerSlot(i);
            mp.setMatch(match);

            players.add(mp);
        }

        match.setMatchPlayers(players);

        assertEquals("C", match.getNameForMatchPlayerSlot(3));
    }

    @Test
    void testGetNameForMatchPlayerSlot_invalid() {
        match.setMatchPlayerNames(List.of("A", "B", "C", "D"));
        assertThrows(IllegalArgumentException.class, () -> match.getNameForMatchPlayerSlot(5));
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
