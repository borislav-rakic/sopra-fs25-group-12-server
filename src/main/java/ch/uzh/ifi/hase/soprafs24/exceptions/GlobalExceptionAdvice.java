package ch.uzh.ifi.hase.soprafs24.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice(annotations = RestController.class)
public class GlobalExceptionAdvice extends ResponseEntityExceptionHandler {

  private final Logger log = LoggerFactory.getLogger(GlobalExceptionAdvice.class);

  // Used to toggle trace information
  @Value("${spring.profiles.active:}")
  private String profile;

  private Map<String, Object> buildErrorBody(String message, HttpStatus status, Exception ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status.value());
    body.put("error", message);
    body.put("exception", ex.getClass().getSimpleName());

    // Optional: include stack trace only in dev
    if ("dev".equalsIgnoreCase(profile)) {
      body.put("trace", ex.getStackTrace());
    }

    return body;
  }

  @ExceptionHandler(value = { IllegalArgumentException.class, IllegalStateException.class })
  protected ResponseEntity<Object> handleConflict(RuntimeException ex, WebRequest request) {
    log.warn("Conflict occurred: {}", ex.getMessage());
    Map<String, Object> body = buildErrorBody(ex.getMessage(), HttpStatus.CONFLICT, ex);
    return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(TransactionSystemException.class)
  public ResponseEntity<Object> handleTransactionSystemException(Exception ex, HttpServletRequest request) {
    log.error("Request: {} raised TransactionSystemException: {}", request.getRequestURL(), ex.getMessage());
    Map<String, Object> body = buildErrorBody(ex.getMessage(), HttpStatus.CONFLICT, ex);
    return new ResponseEntity<>(body, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
    log.warn("Caught ResponseStatusException: {}", ex.getReason());
    Map<String, Object> body = buildErrorBody(ex.getReason(), ex.getStatus(), ex);
    return new ResponseEntity<>(body, ex.getStatus());
  }

  @ExceptionHandler(HttpServerErrorException.InternalServerError.class)
  public ResponseEntity<Object> handleInternalServerError(Exception ex) {
    log.error("InternalServerError caught: {}", ex.getMessage(), ex);
    Map<String, Object> body = buildErrorBody("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR, ex);
    return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleAllUnhandledExceptions(Exception ex) {
    log.error("Unhandled exception: {}", ex.getMessage(), ex);
    Map<String, Object> body = buildErrorBody("Unexpected error", HttpStatus.INTERNAL_SERVER_ERROR, ex);
    return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(GameplayException.class)
  public ResponseEntity<Object> handleGameplayException(GameplayException ex) {
    log.info("Gameplay rule rejection: {}", ex.getMessage());

    Map<String, Object> body = Map.of(
        "status", "rejected",
        "reason", ex.getMessage());

    return ResponseEntity.ok(body); // Not a failure — it's a game state response
  }
}
