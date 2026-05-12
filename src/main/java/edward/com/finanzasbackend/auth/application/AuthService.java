package edward.com.finanzasbackend.auth.application;

import edward.com.finanzasbackend.auth.api.dto.UserDetailsDao;
import edward.com.finanzasbackend.auth.api.dto.*;
import edward.com.finanzasbackend.auth.domain.*;
import edward.com.finanzasbackend.auth.domain.exception.*;
import edward.com.finanzasbackend.auth.infrastructure.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       VerificationTokenRepository verificationTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       TokenService tokenService,
                       EmailService emailService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UUID register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        userRepository.save(user);

        String tokenValue = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken();
        vt.setToken(tokenValue);
        vt.setUser(user);
        vt.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationTokenRepository.save(vt);

        emailService.sendVerificationEmail(user, tokenValue);
        return user.getId();
    }

    @Transactional(readOnly = true)
    public UserDetailsDao getUserDetails(String email){
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            UserDetailsDao userDetailsDao = new UserDetailsDao(
                    user.get().getName(),user.get().getEmail(),user.get().getStatus()
            );
            return userDetailsDao;
        }
        return null;
    }


    @Transactional
    public void verifyEmail(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Verification token not found."));
        if (vt.isUsed()) {
            throw new InvalidTokenException("Verification token has already been used.");
        }
        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Verification token has expired.");
        }
        vt.setUsed(true);
        vt.getUser().setStatus(UserStatus.ACTIVE);
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + request.email()));
        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            return;
        }
        verificationTokenRepository.deleteByUser(user);

        String tokenValue = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken();
        vt.setToken(tokenValue);
        vt.setUser(user);
        vt.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationTokenRepository.save(vt);

        emailService.sendVerificationEmail(user, tokenValue);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new EmailNotVerifiedException();
        }
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, 900L);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshRequest request) {
        RefreshToken rt = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found."));
        if (rt.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked.");
        }
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired.");
        }
        String newAccessToken = tokenService.generateAccessToken(rt.getUser());
        return new AuthResponse(newAccessToken, request.refreshToken(), 900L);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken()).ifPresent(rt -> rt.setRevoked(true));
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUser(user);
            String tokenValue = UUID.randomUUID().toString();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setToken(tokenValue);
            prt.setUser(user);
            prt.setExpiresAt(LocalDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(prt);
            emailService.sendPasswordResetEmail(user, tokenValue);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Password reset token not found."));
        if (prt.isUsed()) {
            throw new InvalidTokenException("Password reset token has already been used.");
        }
        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Password reset token has expired.");
        }
        prt.setUsed(true);
        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        tokenService.revokeAllRefreshTokens(user.getId());
    }
}
