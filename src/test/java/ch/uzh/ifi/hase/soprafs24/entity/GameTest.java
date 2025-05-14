package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.GamePhase;
import ch.uzh.ifi.hase.soprafs24.constant.TrickPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private Game game;

    @BeforeEach
    void setup() {
        game = new Game();
    }

    @Test
    void testAddAndClearCurrentTrick() {
        game.addCardCodeToCurrentTrick("2C");
        game.addCardCodeToCurrentTrick("3D");

        List<String> trick = game.getCurrentTrick();
        assertEquals(2, trick.size());
        assertEquals("2C", trick.get(0));
        assertEquals("3D", trick.get(1));

        game.clearCurrentTrick();
        assertTrue(game.getCurrentTrick().isEmpty());
    }

    @Test
    void testAddCardCodeInvalidFormatThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> game.addCardCodeToCurrentTrick("invalid"));
    }

    @Test
    void testAddMoreThanFourCardsThrowsException() {
        game.addCardCodeToCurrentTrick("2C");
        game.addCardCodeToCurrentTrick("3D");
        game.addCardCodeToCurrentTrick("QH");
        game.addCardCodeToCurrentTrick("AS");
        assertThrows(IllegalStateException.class, () -> game.addCardCodeToCurrentTrick("KH"));
    }

    @Test
    void testGetSuitOfFirstCard() {
        game.setCurrentTrick(Arrays.asList("2C", "3D"));
        assertEquals("C", game.getSuitOfFirstCardInCurrentTrick());
    }

    @Test
    void testTrickMatchPlayerSlotOrder() {
        game.setTrickLeaderMatchPlayerSlot(3);
        List<Integer> order = game.getTrickMatchPlayerSlotOrder();
        assertEquals(Arrays.asList(3, 4, 1, 2), order);
    }

    @Test
    void testGetGameScoresListAndScoreForSlot() {
        game.setGameScoresList(Arrays.asList(4, 5, 6, 7));
        assertEquals(6, game.getScoreForSlot(3));
    }

    @Test
    void testGetScoreForInvalidSlotThrowsException() {
        game.setGameScoresList(Arrays.asList(4, 5, 6, 7));
        assertThrows(IllegalArgumentException.class, () -> game.getScoreForSlot(5));
    }

    @Test
    void testCopyConstructorIncrementsGameNumber() {
        game.setGameNumber(1);
        game.setDeckId("deck123");
        game.setTrickLeaderMatchPlayerSlot(2);
        game.setHeartsBroken(true);

        Game copied = new Game(game);
        assertEquals(2, copied.getGameNumber());
        assertEquals("deck123", copied.getDeckId());
        assertEquals(2, copied.getTrickLeaderMatchPlayerSlot());
        assertTrue(copied.getHeartsBroken());
    }

    @Test
    void testDefaultValues() {
        assertEquals(GamePhase.PRESTART, game.getPhase());
        assertEquals(TrickPhase.READYFORFIRSTCARD, game.getTrickPhase());
        assertEquals(List.of(0, 0, 0, 0), game.getGameScoresList());
        assertEquals(1, game.getCurrentMatchPlayerSlot());
    }

    @Test
    void testGetCardCodeInCurrentTrickAndOutOfBounds() {
        game.setCurrentTrick(List.of("2C", "3D", "QH"));
        assertEquals("3D", game.getCardCodeInCurrentTrick(1));
        assertThrows(IndexOutOfBoundsException.class, () -> game.getCardCodeInCurrentTrick(5));
    }
}
