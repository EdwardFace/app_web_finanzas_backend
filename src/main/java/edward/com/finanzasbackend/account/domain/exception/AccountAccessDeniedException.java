package edward.com.finanzasbackend.account.domain.exception;

public class AccountAccessDeniedException extends RuntimeException {
    public AccountAccessDeniedException() {
        super("Access denied to this account");
    }
}
