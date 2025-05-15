package ch.uzh.ifi.hase.soprafs24.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CardResponseTest {

    @Test
    void testGettersAndSetters() {
        CardResponse card = new CardResponse();

        card.setCode("AH");
        card.setImage("https://deckofcardsapi.com/static/img/AH.png");
        card.setValue("ACE");
        card.setSuit("HEARTS");

        assertEquals("AH", card.getCode());
        assertEquals("https://deckofcardsapi.com/static/img/AH.png", card.getImage());
        assertEquals("ACE", card.getValue());
        assertEquals("HEARTS", card.getSuit());
    }
}
