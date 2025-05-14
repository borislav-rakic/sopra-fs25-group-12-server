package ch.uzh.ifi.hase.soprafs24.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PassedCardTest {

    @Test
    void testNoArgsConstructor() {
        PassedCard card = new PassedCard();
        assertNull(card.getGame());
        assertNull(card.getRankSuit());
        assertEquals(0, card.getFromMatchPlayerSlot());
        assertEquals(0, card.getGameNumber());
    }

    @Test
    void testAllArgsConstructor() {
        Game mockGame = new Game();
        PassedCard card = new PassedCard(mockGame, "QH", 2, 5);

        assertEquals(mockGame, card.getGame());
        assertEquals("QH", card.getRankSuit());
        assertEquals(2, card.getFromMatchPlayerSlot());
        assertEquals(5, card.getGameNumber());
    }

    @Test
    void testSettersAndGetters() {
        Game game = new Game();
        PassedCard card = new PassedCard();

        card.setId(123L);
        card.setGame(game);
        card.setRankSuit("KH");
        card.setFromMatchPlayerSlot(3);
        card.setGameNumber(7);

        assertEquals(123L, card.getId());
        assertEquals(game, card.getGame());
        assertEquals("KH", card.getRankSuit());
        assertEquals(3, card.getFromMatchPlayerSlot());
        assertEquals(7, card.getGameNumber());
    }
}
