package ch.uzh.ifi.hase.soprafs24.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class GameplayExceptionTest {

    @Test
    void constructor_withDefaultStatus_setsConflictStatus() {
        GameplayException ex = new GameplayException("Invalid move");

        assertEquals("Invalid move", ex.getMessage());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void constructor_withCustomStatus_setsGivenStatus() {
        GameplayException ex = new GameplayException("Not allowed", HttpStatus.FORBIDDEN);

        assertEquals("Not allowed", ex.getMessage());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }
}
