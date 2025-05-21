package ch.uzh.ifi.hase.soprafs24.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CardTrickTest {

    @Test
    void testConstructorWithNullInput() {
        CardTrick trick = new CardTrick(null);
        assertEquals(0, trick.size());
    }

    @Test
    void testConstructorWithBlankInput() {
        CardTrick trick = new CardTrick("   ");
        assertEquals(0, trick.size());
    }

    @Test
    void testConstructorWithValidInput() {
        CardTrick trick = new CardTrick("A,K,Q");
        assertEquals(3, trick.size());
        assertEquals("A", trick.getCard(0));
        assertEquals("K", trick.getCard(1));
        assertEquals("Q", trick.getCard(2));
    }

    @Test
    void testAddCardCodeUnderLimit() {
        CardTrick trick = new CardTrick("");
        trick.addCardCode("AS");
        trick.addCardCode("KC");
        trick.addCardCode("QD");
        trick.addCardCode("JC");
        assertEquals(4, trick.size());
    }

    @Test
    void testAddCardCodeOverLimitThrows() {
        CardTrick trick = new CardTrick("A,K,Q,J");
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            trick.addCardCode("10");
        });
        assertEquals("Trick already has 4 cards.", exception.getMessage());
    }

    @Test
    void testAsListReturnsCopy() {
        CardTrick trick = new CardTrick("A,K");
        List<String> list = trick.asList();
        list.add("Injected");
        assertEquals(2, trick.size()); // Internal list should not change
    }

    @Test
    void testClear() {
        CardTrick trick = new CardTrick("A,K");
        trick.clear();
        assertEquals(0, trick.size());
    }

    @Test
    void testAsString() {
        CardTrick trick = new CardTrick("A,K,Q");
        assertEquals("A,K,Q", trick.asString());
    }

    @Test
    void testAsStringEmpty() {
        CardTrick trick = new CardTrick("");
        assertEquals("", trick.asString());
    }

    @Test
    void testGetCard() {
        CardTrick trick = new CardTrick("A,K");
        assertEquals("A", trick.getCard(0));
        assertEquals("K", trick.getCard(1));
    }
}
