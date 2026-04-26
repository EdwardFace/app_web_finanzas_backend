package edward.com.finanzasbackend.auth.infrastructure;

import edward.com.finanzasbackend.auth.domain.PasswordResetToken;
import edward.com.finanzasbackend.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
}
