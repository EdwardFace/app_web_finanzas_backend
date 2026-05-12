package edward.com.finanzasbackend.account.api;

import edward.com.finanzasbackend.account.api.dto.AccountResponse;
import edward.com.finanzasbackend.account.api.dto.CreateAccountRequest;
import edward.com.finanzasbackend.account.api.dto.UpdateAccountRequest;
import edward.com.finanzasbackend.account.application.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request,
                                           Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.create(request, userId(authentication)));
    }

    @GetMapping
    ResponseEntity<List<AccountResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(accountService.findAll(userId(authentication)));
    }

    @GetMapping("/{id}")
    ResponseEntity<AccountResponse> findById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(accountService.findById(id, userId(authentication)));
    }

    @PutMapping("/{id}")
    ResponseEntity<AccountResponse> update(@PathVariable Long id,
                                           @RequestBody UpdateAccountRequest request,
                                           Authentication authentication) {
        return ResponseEntity.ok(accountService.update(id, request, userId(authentication)));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        accountService.delete(id, userId(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
