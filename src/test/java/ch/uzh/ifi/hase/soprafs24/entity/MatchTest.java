package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.MatchPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
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

    @Test
    void testContainsPlayer() {
        User user = new User();
        user.setId(10L);
        match.setPlayer1(user);

        assertTrue(match.containsPlayer(10L));
        assertFalse(match.containsPlayer(99L));
    }

    @Test
    void testGetSlotByPlayerId() {
        User user = new User();
        user.setId(10L);
        match.setPlayer3(user);

        assertEquals(3, match.getSlotByPlayerId(10L));
        assertEquals(-1, match.getSlotByPlayerId(99L));
    }

    @Test
    void testRequireSlotByUser_valid() {
        User user = new User();
        user.setId(77L);
        match.setPlayer2(user);

        assertEquals(2, match.requireSlotByUser(user));
    }

    @Test
    void testRequireSlotByUser_invalid() {
        User user = new User();
        user.setId(123L);

        assertThrows(IllegalArgumentException.class, () -> match.requireSlotByUser(user));
    }

    @Test
    void testRequireUserBySlot_valid() {
        User user = new User();
        user.setId(1L);

        MatchPlayer mp = new MatchPlayer();
        mp.setUser(user);
        mp.setMatchPlayerSlot(1);
        match.setMatchPlayers(List.of(mp));

        assertEquals(user, match.requireUserBySlot(1));
    }

    @Test
    void testRequireUserBySlot_invalid() {
        assertThrows(IllegalArgumentException.class, () -> match.requireUserBySlot(1));
    }

    @Test
    void testRequireMatchPlayerByUser_valid() {
        User user = new User();
        user.setId(5L);

        MatchPlayer mp = new MatchPlayer();
        mp.setUser(user);
        match.setMatchPlayers(List.of(mp));

        assertEquals(mp, match.requireMatchPlayerByUser(user));
    }

    @Test
    void testRequireMatchPlayerByUser_invalid() {
        User user = new User();
        user.setId(999L);

        assertThrows(IllegalArgumentException.class, () -> match.requireMatchPlayerByUser(user));
    }

    @Test
    void testRequireMatchPlayerByToken_valid() {
        User user = new User();
        user.setToken("abc123");

        MatchPlayer mp = new MatchPlayer();
        mp.setUser(user);
        match.setMatchPlayers(List.of(mp));

        assertEquals(mp, match.requireMatchPlayerByToken("abc123"));
    }

    @Test
    void testRequireMatchPlayerByToken_invalid() {
        assertThrows(IllegalArgumentException.class, () -> match.requireMatchPlayerByToken("wrong-token"));
    }

    @Test
    void testRequireMatchPlayerBySlot_valid() {
        MatchPlayer mp = new MatchPlayer();
        mp.setMatchPlayerSlot(2);
        match.setMatchPlayers(List.of(mp));

        assertEquals(mp, match.requireMatchPlayerBySlot(2));
    }

    @Test
    void testRequireMatchPlayerBySlot_invalid() {
        assertThrows(IllegalStateException.class, () -> match.requireMatchPlayerBySlot(5));
    }

    @Test
    void testRequireHostPlayer_valid() {
        MatchPlayer mp = new MatchPlayer();
        mp.setIsHost(true);
        match.setMatchPlayers(List.of(mp));

        assertEquals(mp, match.requireHostPlayer());
    }

    @Test
    void testRequireHostPlayer_invalid() {
        assertThrows(IllegalStateException.class, () -> match.requireHostPlayer());
    }

    @Test
    void testRemoveAllAiPlayers() {
        Map<Integer, Integer> aiMap = new HashMap<>();
        aiMap.put(1, 2);
        aiMap.put(2, 3);

        match.setAiPlayers(aiMap);
        match.removeAllAiPlayers();

        assertTrue(match.getAiPlayers().isEmpty());
    }

    @Test
    void testGetMatchPlayersSortedBySlot() {
        MatchPlayer mp1 = new MatchPlayer();
        mp1.setMatchPlayerSlot(3);

        MatchPlayer mp2 = new MatchPlayer();
        mp2.setMatchPlayerSlot(1);

        MatchPlayer mp3 = new MatchPlayer();
        mp3.setMatchPlayerSlot(2);

        match.setMatchPlayers(List.of(mp1, mp2, mp3));
        List<MatchPlayer> sorted = match.getMatchPlayersSortedBySlot();

        assertEquals(1, sorted.get(0).getMatchPlayerSlot());
        assertEquals(2, sorted.get(1).getMatchPlayerSlot());
        assertEquals(3, sorted.get(2).getMatchPlayerSlot());
    }

    @Test
    void testFindPlayersExceptSlot() {
        MatchPlayer mp1 = new MatchPlayer();
        mp1.setMatchPlayerSlot(1);

        MatchPlayer mp2 = new MatchPlayer();
        mp2.setMatchPlayerSlot(2);

        match.setMatchPlayers(List.of(mp1, mp2));

        List<MatchPlayer> result = match.findPlayersExceptSlot(1);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getMatchPlayerSlot());
    }

    @Test
    void testAddGame() {
        Game game = new Game();
        match.addGame(game);

        assertEquals(1, match.getGames().size());
        assertEquals(match, game.getMatch());
    }

    @Test
    void testGetMatchScoresMap() {
        match.setMatchScoresList(List.of(7, 8, 9, 10));
        Map<Integer, Integer> map = match.getMatchScoresMap();

        assertEquals(4, map.size());
        assertEquals(9, map.get(2));
    }

    @Test
    void testCollectCurrentGameScores() {
        MatchPlayer mp1 = new MatchPlayer();
        mp1.setMatchPlayerSlot(1);
        mp1.setGameScore(5);

        MatchPlayer mp2 = new MatchPlayer();
        mp2.setMatchPlayerSlot(2);
        mp2.setGameScore(3);

        match.setMatchPlayers(List.of(mp1, mp2));
        Map<Integer, Integer> scores = match.collectCurrentGameScores();

        assertEquals(5, scores.get(1));
        assertEquals(3, scores.get(2));
    }

    @Test
    void testSetAndGetMatchSummary() {
        MatchSummary summary = new MatchSummary();
        match.setMatchSummary(summary);

        assertEquals(summary, match.getMatchSummary());
    }

    @Test
    void testSlotDidConfirmLastGameCsvHandling() {
        match.setSlotDidConfirmLastGame(List.of(1, 2, 4));
        List<Integer> result = match.getSlotDidConfirmLastGame();

        assertEquals(3, result.size());
        assertTrue(result.contains(4));
    }

}
