package io.quintkard.quintkardapp.message;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MessageExceptionHandler {

    @ExceptionHandler(MessageNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleMessageNotFound(MessageNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationFailure(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Request validation failed."));
    }
}
