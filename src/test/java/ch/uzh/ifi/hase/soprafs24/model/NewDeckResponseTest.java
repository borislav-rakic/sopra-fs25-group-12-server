package ch.uzh.ifi.hase.soprafs24.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NewDeckResponseTest {

    @Test
    void testGettersAndSetters() {
        NewDeckResponse response = new NewDeckResponse();

        response.setSuccess(true);
        response.setDeck_id("deck789");
        response.setShuffled(true);
        response.setRemaining(52);

        assertTrue(response.isSuccess());
        assertEquals("deck789", response.getDeck_id());
        assertTrue(response.isShuffled());
        assertEquals(52, response.getRemaining());
    }
}
