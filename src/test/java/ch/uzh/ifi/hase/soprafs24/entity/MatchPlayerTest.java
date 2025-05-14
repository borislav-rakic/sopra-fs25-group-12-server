package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.AiMatchPlayerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchPlayerTest {

    private MatchPlayer player;

    @BeforeEach
    void setup() {
        player = new MatchPlayer();
    }

    @Test
    void testAddAndRemoveCardFromHand() {
        player.addCardCodeToHand("2C");
        player.addCardCodeToHand("QH");

        assertTrue(player.hasCardCodeInHand("QH"));
        assertEquals(2, player.numberOfCardsInHand());

        assertTrue(player.removeCardCodeFromHand("2C"));
        assertFalse(player.hasCardCodeInHand("2C"));
        assertEquals(1, player.numberOfCardsInHand());

        assertFalse(player.removeCardCodeFromHand("ZZ"));
    }

    @Test
    void testClearHand() {
        player.addCardCodeToHand("AS");
        player.clearHand();
        assertEquals(0, player.numberOfCardsInHand());
    }

    @Test
    void testHandFormatValidation_valid() {
        player.setHand("2C,3D,QH");
        assertTrue(player.isProperHandFormat());
        assertTrue(player.hasNoDuplicateCards());
        assertTrue(player.isValidHand());
    }

    @Test
    void testHandFormatValidation_duplicate_invalid() {
        player.setHand("2C,3D,3D");
        assertFalse(player.hasNoDuplicateCards());
        assertFalse(player.isValidHand());
    }

    @Test
    void testGetCardsOfSuitAndNotOfSuit() {
        player.setHand("2C,3D,KH,AS");

        assertEquals("2C", player.getCardsOfSuit('C'));
        assertEquals("3D,KH,AS", player.getCardsNotOfSuit('C'));

        assertEquals("KH", player.getCardsOfSuit('H'));
        assertEquals("2C,3D,AS", player.getCardsNotOfSuit('H'));
    }

    @Test
    void testSortHand() {
        player.setHand("QS,2C,KH,3D");
        player.sortHand();
        // C < D < S < H
        // 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 < 0 < J < Q < K < A
        assertEquals("2C,3D,QS,KH", player.getHand());
    }

    @Test
    void testScoreOperations() {
        player.setGameScore(10);
        player.setMatchScore(20);

        player.addToGameScore(5);
        player.addToMatchScore(15);

        assertEquals(15, player.getGameScore());
        assertEquals(35, player.getMatchScore());

        player.resetGameScore();
        player.resetMatchStats();

        assertEquals(0, player.getGameScore());
        assertEquals(0, player.getMatchScore());
        assertEquals(0, player.getPerfectGames());
        assertEquals(0, player.getShotTheMoonCount());
    }

    @Test
    void testMoonAndPerfectTracking() {
        player.incrementPerfectGames();
        player.incrementPerfectGames();
        assertEquals(2, player.getPerfectGames());

        player.incrementShotTheMoonCount();
        assertEquals(1, player.getShotTheMoonCount());

        player.resetPerfectGames();
        player.resetShotTheMoonCount();
        assertEquals(0, player.getPerfectGames());
        assertEquals(0, player.getShotTheMoonCount());
    }

    @Test
    void testTakenCards() {
        player.addTakenCard("QH");
        player.addTakenCard("AS");

        List<String> taken = player.getTakenCards();
        assertEquals(2, taken.size());
        assertTrue(taken.contains("QH"));
        assertTrue(taken.contains("AS"));

        player.setTakenCards(List.of("2C", "3D"));
        assertEquals(2, player.getTakenCards().size());
        assertTrue(player.getTakenCards().contains("3D"));
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
        player.setHand("2C,3D,KH");
        String[] cards = player.getHandCardsArray();
        assertArrayEquals(new String[] { "2C", "3D", "KH" }, cards);
    }
}
