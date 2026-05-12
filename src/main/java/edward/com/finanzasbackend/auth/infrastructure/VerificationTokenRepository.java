package edward.com.finanzasbackend.auth.infrastructure;

import edward.com.finanzasbackend.auth.domain.User;
import edward.com.finanzasbackend.auth.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);
    void deleteByUser(User user);
}
