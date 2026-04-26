package edward.com.finanzasbackend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import edward.com.finanzasbackend.auth.domain.PasswordResetToken;
import edward.com.finanzasbackend.auth.domain.User;
import edward.com.finanzasbackend.auth.domain.UserStatus;
import edward.com.finanzasbackend.auth.infrastructure.PasswordResetTokenRepository;
import edward.com.finanzasbackend.auth.infrastructure.RefreshTokenRepository;
import edward.com.finanzasbackend.auth.infrastructure.UserRepository;
import edward.com.finanzasbackend.auth.infrastructure.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class PasswordResetIT {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired VerificationTokenRepository verificationTokenRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean JavaMailSender mailSender;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
    }

    private User createActiveUser(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setName("Test User");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Test
    void scenario1_forgotPasswordExistingEmail_returns200() throws Exception {
        createActiveUser("user@example.com", "password1");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void scenario2_forgotPasswordNonExistingEmail_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ghost@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void scenario3_resetPasswordWithValidToken_updatesPassword() throws Exception {
        User user = createActiveUser("reset@example.com", "oldpassword");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"reset@example.com"}
                                """))
                .andExpect(status().isOk());

        PasswordResetToken prt = passwordResetTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + prt.getToken() + "\",\"newPassword\":\"newpassword\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"reset@example.com","password":"newpassword"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void scenario4_resetPasswordWithUsedToken_returns400() throws Exception {
        User user = createActiveUser("reuse@example.com", "password1");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"reuse@example.com"}
                                """))
                .andExpect(status().isOk());

        PasswordResetToken prt = passwordResetTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + prt.getToken() + "\",\"newPassword\":\"newpassword\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + prt.getToken() + "\",\"newPassword\":\"anotherpass\"}"))
                .andExpect(status().isBadRequest());
    }
}
