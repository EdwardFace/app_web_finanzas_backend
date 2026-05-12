package edward.com.finanzasbackend.account.api;

import edward.com.finanzasbackend.account.domain.exception.AccountNotFoundException;
import edward.com.finanzasbackend.account.domain.exception.AccountAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<Map<String, String>> handleAccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
    }

    @ExceptionHandler(AccountAccessDeniedException.class)
    ResponseEntity<Map<String, String>> handleAccountAccessDenied(AccountAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(ex.getMessage()));
    }

    private Map<String, String> error(String message) {
        return Map.of("error", message);
    }


}