package edward.com.finanzasbackend.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edward.com.finanzasbackend.auth.api.dto.*;
import edward.com.finanzasbackend.auth.application.AuthService;
import edward.com.finanzasbackend.auth.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock AuthService authService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_returns201WithUserId() throws Exception {
        UUID id = UUID.randomUUID();
        when(authService.register(any())).thenReturn(id);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Alice", "alice@example.com", "password1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(id.toString()));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("alice@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Alice", "alice@example.com", "password1"))))
                .andExpect(status().isConflict());
    }

    @Test
    void verifyEmail_returns200WithMessage() throws Exception {
        doNothing().when(authService).verifyEmail("tok");

        mockMvc.perform(get("/api/auth/verify-email").param("token", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void verifyEmail_invalidToken_returns400() throws Exception {
        doThrow(new InvalidTokenException("bad token")).when(authService).verifyEmail("bad");

        mockMvc.perform(get("/api/auth/verify-email").param("token", "bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_success_returns200WithTokens() throws Exception {
        AuthResponse resp = new AuthResponse("access", "refresh", 900L);
        when(authService.login(any())).thenReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@example.com", "password1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void login_unverified_returns403() throws Exception {
        when(authService.login(any())).thenThrow(new EmailNotVerifiedException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@example.com", "password1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@example.com", "wrongpass"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_success_returns200() throws Exception {
        when(authService.refreshToken(any())).thenReturn(new AuthResponse("new-access", "refresh", 900L));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    void logout_returns204() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogoutRequest("refresh"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void forgotPassword_returns200() throws Exception {
        doNothing().when(authService).forgotPassword(any());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("alice@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void resetPassword_returns200() throws Exception {
        doNothing().when(authService).resetPassword(any());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest("tok", "newpassword"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
