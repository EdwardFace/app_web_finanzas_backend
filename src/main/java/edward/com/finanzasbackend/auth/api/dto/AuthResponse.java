package edward.com.finanzasbackend.auth.api.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
