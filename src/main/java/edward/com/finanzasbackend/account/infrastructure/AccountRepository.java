package edward.com.finanzasbackend.account.infrastructure;

import edward.com.finanzasbackend.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserIdAndDeletedAtIsNull(UUID userId);
    Optional<Account> findByIdAndDeletedAtIsNull(Long id);
}
