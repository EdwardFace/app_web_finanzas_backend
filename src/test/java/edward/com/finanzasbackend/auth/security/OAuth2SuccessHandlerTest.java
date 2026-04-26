package edward.com.finanzasbackend.auth.security;

import edward.com.finanzasbackend.auth.application.TokenService;
import edward.com.finanzasbackend.auth.domain.User;
import edward.com.finanzasbackend.auth.domain.UserStatus;
import edward.com.finanzasbackend.auth.infrastructure.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock UserRepository userRepository;
    @Mock TokenService tokenService;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock Authentication authentication;
    @Mock OAuth2User oAuth2User;

    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(userRepository, tokenService, "http://localhost:3000");
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("google@example.com");
        when(oAuth2User.getName()).thenReturn("google-id-123");
        when(oAuth2User.getAttribute("name")).thenReturn("Google User");
        when(tokenService.generateAccessToken(any())).thenReturn("access-tok");
        when(tokenService.generateRefreshToken(any())).thenReturn("refresh-tok");
        when(response.encodeRedirectURL(anyString())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void newUser_isCreatedWithActiveStatus() throws Exception {
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getGoogleId()).isEqualTo("google-id-123");
        verify(response).sendRedirect(contains("accessToken=access-tok"));
    }

    @Test
    void existingUserWithoutGoogleId_getsLinked() throws Exception {
        User existing = new User();
        existing.setId(UUID.randomUUID());
        existing.setEmail("google@example.com");
        existing.setStatus(UserStatus.ACTIVE);
        // googleId is null

        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(existing.getGoogleId()).isEqualTo("google-id-123");
        verify(response).sendRedirect(contains("refreshToken=refresh-tok"));
    }
}
