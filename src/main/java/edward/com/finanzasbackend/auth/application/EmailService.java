package edward.com.finanzasbackend.auth.application;

import edward.com.finanzasbackend.auth.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
    }

    public void sendVerificationEmail(User user, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Verify your email - FinanzasApp");
        message.setText("Hello " + user.getName() + ",\n\n"
                + "Please verify your email address by clicking the link below:\n"
                + link + "\n\n"
                + "This link expires in 24 hours.\n\n"
                + "If you did not register, please ignore this email.");
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(User user, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Reset your password - FinanzasApp");
        message.setText("Hello " + user.getName() + ",\n\n"
                + "You requested a password reset. Click the link below to set a new password:\n"
                + link + "\n\n"
                + "This link expires in 1 hour.\n\n"
                + "If you did not request a password reset, please ignore this email.");
        mailSender.send(message);
    }
}
