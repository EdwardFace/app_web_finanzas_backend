package edward.com.finanzasbackend.auth.application;

import edward.com.finanzasbackend.auth.api.dto.*;
import edward.com.finanzasbackend.auth.domain.*;
import edward.com.finanzasbackend.auth.domain.exception.*;
import edward.com.finanzasbackend.auth.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock VerificationTokenRepository verificationTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock TokenService tokenService;
    @Mock EmailService emailService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthService authService;

    private User activeUser;
    private User pendingUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(UUID.randomUUID());
        activeUser.setEmail("active@example.com");
        activeUser.setName("Active User");
        activeUser.setPasswordHash("$2a$10$hashedpassword");
        activeUser.setStatus(UserStatus.ACTIVE);

        pendingUser = new User();
        pendingUser.setId(UUID.randomUUID());
        pendingUser.setEmail("pending@example.com");
        pendingUser.setName("Pending User");
        pendingUser.setPasswordHash("$2a$10$hashedpassword");
        pendingUser.setStatus(UserStatus.PENDING_VERIFICATION);
    }

    // --- register ---

    @Test
    void register_success() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UUID id = authService.register(new RegisterRequest("New", "new@example.com", "password1"));

        assertThat(id).isNotNull();
        verify(emailService).sendVerificationEmail(any(), anyString());
    }

    @Test
    void register_duplicateEmail_throwsEmailAlreadyExists() {
        when(userRepository.existsByEmail("active@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("X", "active@example.com", "password1")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    // --- verifyEmail ---

    @Test
    void verifyEmail_success() {
        VerificationToken vt = new VerificationToken();
        vt.setToken("tok");
        vt.setUser(pendingUser);
        vt.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(verificationTokenRepository.findByToken("tok")).thenReturn(Optional.of(vt));

        authService.verifyEmail("tok");

        assertThat(vt.isUsed()).isTrue();
        assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void verifyEmail_tokenNotFound_throwsInvalidToken() {
        when(verificationTokenRepository.findByToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("bad"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_tokenAlreadyUsed_throwsInvalidToken() {
        VerificationToken vt = new VerificationToken();
        vt.setToken("used");
        vt.setUsed(true);
        vt.setUser(activeUser);
        vt.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(verificationTokenRepository.findByToken("used")).thenReturn(Optional.of(vt));

        assertThatThrownBy(() -> authService.verifyEmail("used"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void verifyEmail_tokenExpired_throwsInvalidToken() {
        VerificationToken vt = new VerificationToken();
        vt.setToken("exp");
        vt.setUser(pendingUser);
        vt.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(verificationTokenRepository.findByToken("exp")).thenReturn(Optional.of(vt));

        assertThatThrownBy(() -> authService.verifyEmail("exp"))
                .isInstanceOf(InvalidTokenException.class);
    }

    // --- login ---

    @Test
    void login_success() {
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("plainpass", "$2a$10$hashedpassword")).thenReturn(true);
        when(tokenService.generateAccessToken(activeUser)).thenReturn("access");
        when(tokenService.generateRefreshToken(activeUser)).thenReturn("refresh");

        AuthResponse resp = authService.login(new LoginRequest("active@example.com", "plainpass"));

        assertThat(resp.accessToken()).isEqualTo("access");
        assertThat(resp.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong", "$2a$10$hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("active@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unverifiedAccount_throwsEmailNotVerified() {
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(pendingUser));
        when(passwordEncoder.matches("plainpass", "$2a$10$hashedpassword")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("pending@example.com", "plainpass")))
                .isInstanceOf(EmailNotVerifiedException.class);
    }

    // --- refreshToken ---

    @Test
    void refreshToken_success() {
        RefreshToken rt = new RefreshToken();
        rt.setToken("valid-refresh");
        rt.setUser(activeUser);
        rt.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("valid-refresh")).thenReturn(Optional.of(rt));
        when(tokenService.generateAccessToken(activeUser)).thenReturn("new-access");

        AuthResponse resp = authService.refreshToken(new RefreshRequest("valid-refresh"));

        assertThat(resp.accessToken()).isEqualTo("new-access");
    }

    @Test
    void refreshToken_revoked_throwsInvalidToken() {
        RefreshToken rt = new RefreshToken();
        rt.setToken("revoked");
        rt.setRevoked(true);
        rt.setUser(activeUser);
        rt.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refreshToken(new RefreshRequest("revoked")))
                .isInstanceOf(InvalidTokenException.class);
    }

    // --- logout ---

    @Test
    void logout_revokesToken() {
        RefreshToken rt = new RefreshToken();
        rt.setToken("tok");
        rt.setUser(activeUser);
        rt.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(rt));

        authService.logout(new LogoutRequest("tok"));

        assertThat(rt.isRevoked()).isTrue();
    }

    // --- forgotPassword ---

    @Test
    void forgotPassword_existingUser_sendsEmail() {
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));

        authService.forgotPassword(new ForgotPasswordRequest("active@example.com"));

        verify(emailService).sendPasswordResetEmail(eq(activeUser), anyString());
    }

    @Test
    void forgotPassword_nonExistingEmail_doesNothing() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(new ForgotPasswordRequest("ghost@example.com"));

        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    // --- resetPassword ---

    @Test
    void resetPassword_success() {
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken("reset-tok");
        prt.setUser(activeUser);
        prt.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(passwordResetTokenRepository.findByToken("reset-tok")).thenReturn(Optional.of(prt));
        when(passwordEncoder.encode("newpassword")).thenReturn("newhash");

        authService.resetPassword(new ResetPasswordRequest("reset-tok", "newpassword"));

        assertThat(prt.isUsed()).isTrue();
        assertThat(activeUser.getPasswordHash()).isEqualTo("newhash");
        verify(tokenService).revokeAllRefreshTokens(activeUser.getId());
    }

    @Test
    void resetPassword_usedToken_throwsInvalidToken() {
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken("used");
        prt.setUsed(true);
        prt.setUser(activeUser);
        prt.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(passwordResetTokenRepository.findByToken("used")).thenReturn(Optional.of(prt));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("used", "newpassword")))
                .isInstanceOf(InvalidTokenException.class);
    }
}
