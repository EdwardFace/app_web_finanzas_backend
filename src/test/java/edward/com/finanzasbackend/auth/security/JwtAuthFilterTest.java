package edward.com.finanzasbackend.auth.security;

import edward.com.finanzasbackend.auth.application.TokenService;
import edward.com.finanzasbackend.auth.domain.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock TokenService tokenService;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain filterChain;
    @Mock Claims claims;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(tokenService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_populatesSecurityContext() throws Exception {
        String userId = UUID.randomUUID().toString();

        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(tokenService.validateAccessToken("valid.token.here")).thenReturn(claims);
        when(claims.getSubject()).thenReturn(userId);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(userId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidToken_doesNotPopulateSecurityContext() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(tokenService.validateAccessToken("invalid.token"))
                .thenThrow(new InvalidTokenException("bad"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void missingAuthHeader_doesNotPopulateSecurityContext() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
