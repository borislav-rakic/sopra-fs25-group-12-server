package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.AiMatchPlayerState;
import ch.uzh.ifi.hase.soprafs24.util.CardUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchPlayerTest {

    private MatchPlayer player;

    @BeforeEach
    void setup() {
        player = new MatchPlayer();
    }

    @Test
    void testScoreOperations() {
        player.setGameScore(10);
        player.setMatchScore(20);

        player.setGameScore(player.getGameScore() + 5);
        player.setMatchScore(player.getMatchScore() + 15);

        assertEquals(15, player.getGameScore());
        assertEquals(35, player.getMatchScore());

        player.setGameScore(0);

        player.setMatchScore(0);
        player.setPerfectGames(0);
        player.setShotTheMoonCount(0);
        player.setTakenCards("");

        assertEquals(0, player.getGameScore());
        assertEquals(0, player.getMatchScore());
        assertEquals(0, player.getPerfectGames());
        assertEquals(0, player.getShotTheMoonCount());
    }

    @Test
    void testMoonAndPerfectTracking() {
        player.setPerfectGames(player.getPerfectGames() + 1);
        player.setPerfectGames(player.getPerfectGames() + 1);
        assertEquals(2, player.getPerfectGames());

        player.incrementShotTheMoonCount();
        assertEquals(1, player.getShotTheMoonCount());

        player.setPerfectGames(0);
        player.resetShotTheMoonCount();
        assertEquals(0, player.getPerfectGames());
        assertEquals(0, player.getShotTheMoonCount());
    }

    @Test
    void testPollCounterAndAiState() {
        assertEquals(0, player.getPollCounter());
        player.incrementPollCounter();
        assertEquals(1, player.getPollCounter());

        assertEquals(AiMatchPlayerState.READY, player.getAiMatchPlayerState());
    }

    @Test
    void testHostFlagAndRanking() {
        player.setIsHost(true);
        assertTrue(player.getIsHost());

        player.setRankingInMatch(2);
        player.setRankingInGame(1);

        assertEquals(2, player.getRankingInMatch());
        assertEquals(1, player.getRankingInGame());
    }

    @Test
    void testGetHandCardsArray() {
        String hand = "2C,3D,KH";
        String[] cards = CardUtils.splitCardCodesAsListOfStrings(hand).toArray(new String[0]);
        assertArrayEquals(new String[] { "2C", "3D", "KH" }, cards);
    }
}
