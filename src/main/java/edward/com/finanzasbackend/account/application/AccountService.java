package edward.com.finanzasbackend.account.application;

import edward.com.finanzasbackend.account.api.dto.AccountResponse;
import edward.com.finanzasbackend.account.api.dto.CreateAccountRequest;
import edward.com.finanzasbackend.account.api.dto.UpdateAccountRequest;
import edward.com.finanzasbackend.account.domain.Account;
import edward.com.finanzasbackend.account.domain.exception.AccountAccessDeniedException;
import edward.com.finanzasbackend.account.domain.exception.AccountNotFoundException;
import edward.com.finanzasbackend.account.infrastructure.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request, UUID userId) {
        Account account = new Account();
        account.setName(request.name());
        account.setType(request.type());
        account.setUserId(userId);
        return toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll(UUID userId) {
        return accountRepository.findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id, UUID userId) {
        Account account = findActiveOrThrow(id);
        checkOwnership(account, userId);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse update(Long id, UpdateAccountRequest request, UUID userId) {
        Account account = findActiveOrThrow(id);
        checkOwnership(account, userId);
        if (request.name() != null) account.setName(request.name());
        if (request.type() != null) account.setType(request.type());
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public void delete(Long id, UUID userId) {
        Account account = findActiveOrThrow(id);
        checkOwnership(account, userId);
        account.setDeletedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    private Account findActiveOrThrow(Long id) {
        return accountRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private void checkOwnership(Account account, UUID userId) {
        if (!account.getUserId().equals(userId)) {
            throw new AccountAccessDeniedException();
        }
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getBalance(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
