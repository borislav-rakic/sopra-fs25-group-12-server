package ch.uzh.ifi.hase.soprafs24.exceptions;

import org.springframework.http.HttpStatus;

public class GameplayException extends RuntimeException {
    private final HttpStatus status;

    public GameplayException(String message) {
        super(message);
        this.status = HttpStatus.CONFLICT; // sensible default
    }

    public GameplayException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}