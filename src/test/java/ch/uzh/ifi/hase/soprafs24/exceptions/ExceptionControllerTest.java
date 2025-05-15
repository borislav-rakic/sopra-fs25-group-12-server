package ch.uzh.ifi.hase.soprafs24.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/test-exception")
public class ExceptionControllerTest {

    @GetMapping("/illegal-argument")
    public void throwIllegalArgument() {
        throw new IllegalArgumentException("This is illegal");
    }

    @GetMapping("/generic")
    public void throwGeneric() {
        throw new RuntimeException("Unexpected error");
    }

    @GetMapping("/response-status")
    public void throwResponseStatus() {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad input");
    }

    @GetMapping("/gameplay")
    public void throwGameplay() {
        throw new GameplayException("You can't play that card");
    }
}
