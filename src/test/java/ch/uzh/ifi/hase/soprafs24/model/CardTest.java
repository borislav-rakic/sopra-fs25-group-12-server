package ch.uzh.ifi.hase.soprafs24.model;

import ch.uzh.ifi.hase.soprafs24.util.CardUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CardTest {

    @Test
    void testSetCodeValidTwoChar() {
        Card card = new Card();
        card.setCode("AH");

        assertEquals("AH", card.getCode());
        assertEquals("A", card.getRank());
        assertEquals("H", card.getSuit());
        assertEquals("Hearts", card.getSuitName());
        assertEquals(14, card.getValue());
        assertEquals("https://deckofcardsapi.com/static/img/AH.png", card.getImage());
        assertEquals(CardUtils.calculateCardOrder("AH"), card.getCardOrder());
    }

    @Test
    void testSetCodeValidThreeChar() {
        Card card = new Card();
        card.setCode("10S");

        assertEquals("10S", card.getCode());
        assertEquals("10", card.getRank());
        assertEquals("S", card.getSuit());
        assertEquals("Spades", card.getSuitName());
        assertEquals(0, card.getValue()); // "10" maps to 0 in calculateValue()
        assertEquals(CardUtils.calculateCardOrder("10S"), card.getCardOrder());
    }

    @Test
    void testSetCodeInvalidNull() {
        Card card = new Card();
        assertThrows(IllegalArgumentException.class, () -> card.setCode(null));
    }

    @Test
    void testSetCodeInvalidTooShort() {
        Card card = new Card();
        assertThrows(IllegalArgumentException.class, () -> card.setCode("A"));
    }

    @Test
    void testSetCodeInvalidTooLong() {
        Card card = new Card();
        assertThrows(IllegalArgumentException.class, () -> card.setCode("1234"));
    }

    @Test
    void testGetSuitNameUnknown() {
        Card card = new Card() {
            {
                // bypass setCode and directly inject invalid suit
                setCode("AH"); // valid to initialize other fields
                try {
                    java.lang.reflect.Field suitField = Card.class.getDeclaredField("suit");
                    suitField.setAccessible(true);
                    suitField.set(this, "Z"); // inject invalid suit
                } catch (Exception e) {
                    throw new RuntimeException("Reflection failed", e);
                }
            }
        };

        assertEquals("Unknown", card.getSuitName());
    }

    @Test
    void testCalculateValueForFaceCards() {
        Card card = new Card();

        card.setCode("JH");
        assertEquals(11, card.getValue());

        card.setCode("QH");
        assertEquals(12, card.getValue());

        card.setCode("KH");
        assertEquals(13, card.getValue());

        card.setCode("AH");
        assertEquals(14, card.getValue());
    }

    @Test
    void testCalculateValueDefault() {
        Card card = new Card();
        card.setCode("0H");
        assertEquals(0, card.getValue()); // Triggers the default branch
    }

}
