package edward.com.finanzasbackend.auth;

import edward.com.finanzasbackend.auth.domain.UserStatus;
import edward.com.finanzasbackend.auth.domain.VerificationToken;
import edward.com.finanzasbackend.auth.infrastructure.UserRepository;
import edward.com.finanzasbackend.auth.infrastructure.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class RegisterAndVerifyIT {

    @Autowired WebApplicationContext context;
    @Autowired UserRepository userRepository;
    @Autowired VerificationTokenRepository verificationTokenRepository;
    @MockitoBean JavaMailSender mailSender;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void scenario1_registerThenVerify_userBecomesActive() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"alice@example.com","password":"password1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists());

        var user = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);

        var vt = verificationTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow();

        mockMvc.perform(get("/api/auth/verify-email").param("token", vt.getToken()))
                .andExpect(status().isOk());

        var verified = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(verified.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void scenario2_duplicateEmail_returns409() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"dup@example.com","password":"password1"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"dup@example.com","password":"password1"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void scenario3_expiredToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bob","email":"bob@example.com","password":"password1"}
                                """))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("bob@example.com").orElseThrow();
        VerificationToken vt = verificationTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst().orElseThrow();
        vt.setExpiresAt(LocalDateTime.now().minusHours(1));
        verificationTokenRepository.save(vt);

        mockMvc.perform(get("/api/auth/verify-email").param("token", vt.getToken()))
                .andExpect(status().isBadRequest());
    }
}
