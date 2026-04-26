package edward.com.finanzasbackend.auth.application;

import edward.com.finanzasbackend.auth.domain.User;
import edward.com.finanzasbackend.auth.domain.UserStatus;
import edward.com.finanzasbackend.auth.domain.exception.InvalidTokenException;
import edward.com.finanzasbackend.auth.infrastructure.RefreshTokenRepository;
import edward.com.finanzasbackend.auth.infrastructure.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;

    private TokenService tokenService;

    private static final String SECRET = "thisisaverysecretkeythathasenoughbitsforhs256algorithm!";

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(refreshTokenRepository, userRepository, SECRET, 900L, 604800L);
    }

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    void generateAndValidateAccessToken_success() {
        User user = buildUser();
        String token = tokenService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        Claims claims = tokenService.validateAccessToken(token);
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo(user.getEmail());
    }

    @Test
    void validateAccessToken_invalidToken_throwsException() {
        assertThatThrownBy(() -> tokenService.validateAccessToken("not.a.valid.token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateAccessToken_tamperedToken_throwsException() {
        User user = buildUser();
        String token = tokenService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> tokenService.validateAccessToken(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }
}
