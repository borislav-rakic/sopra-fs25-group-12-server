package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.MatchMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MatchMessageTest {

    private MatchMessage message;

    @BeforeEach
    void setup() {
        message = new MatchMessage();
    }

    @Test
    void testFieldInitialization() {
        assertNotNull(message.getCreatedAt(), "createdAt should be initialized");
        assertEquals(0, message.getSeenByBitmask());
    }

    @Test
    void testSettersAndGetters() {
        Match mockMatch = new Match();
        message.setMatch(mockMatch);
        message.setType(MatchMessageType.GAME_STARTED);
        message.setContent("Hello, world!");
        message.setSeenByBitmask(3);
        Instant now = Instant.now();
        message.setCreatedAt(now);

        assertEquals(mockMatch, message.getMatch());
        assertEquals(MatchMessageType.GAME_STARTED, message.getType());
        assertEquals("Hello, world!", message.getContent());
        assertEquals(3, message.getSeenByBitmask());
        assertEquals(now, message.getCreatedAt());
    }

    @Test
    void testMarkSeenAndHasSeen() {
        assertFalse(message.hasSeen(1));
        assertFalse(message.hasSeen(2));

        message.markSeen(1);
        assertTrue(message.hasSeen(1));
        assertFalse(message.hasSeen(2));

        message.markSeen(2);
        assertTrue(message.hasSeen(1));
        assertTrue(message.hasSeen(2));
    }
}
