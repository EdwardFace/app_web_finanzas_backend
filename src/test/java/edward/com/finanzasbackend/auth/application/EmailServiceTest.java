package edward.com.finanzasbackend.auth.application;

import edward.com.finanzasbackend.auth.domain.User;
import edward.com.finanzasbackend.auth.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, "http://localhost:3000");
    }

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setName("Jane Doe");
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        return user;
    }

    @Test
    void sendVerificationEmail_callsSendWithCorrectParams() {
        User user = buildUser();
        String token = "abc-token-123";

        emailService.sendVerificationEmail(user, token);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).contains(user.getEmail());
        assertThat(sent.getText()).contains("http://localhost:3000/verify-email?token=abc-token-123");
    }

    @Test
    void sendPasswordResetEmail_callsSendWithCorrectParams() {
        User user = buildUser();
        String token = "reset-token-456";

        emailService.sendPasswordResetEmail(user, token);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).contains(user.getEmail());
        assertThat(sent.getText()).contains("http://localhost:3000/reset-password?token=reset-token-456");
    }
}
