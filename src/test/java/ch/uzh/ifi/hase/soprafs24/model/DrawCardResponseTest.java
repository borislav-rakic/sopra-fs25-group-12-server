package ch.uzh.ifi.hase.soprafs24.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DrawCardResponseTest {

    @Test
    void testGettersAndSetters() {
        DrawCardResponse response = new DrawCardResponse();

        CardResponse card = new CardResponse();
        card.setCode("AH");

        response.setSuccess(true);
        response.setDeck_id("deck123");
        response.setCards(List.of(card));
        response.setRemaining(50);

        assertTrue(response.isSuccess());
        assertEquals("deck123", response.getDeck_id());
        assertEquals(1, response.getCards().size());
        assertEquals("AH", response.getCards().get(0).getCode());
        assertEquals(50, response.getRemaining());
    }
}
