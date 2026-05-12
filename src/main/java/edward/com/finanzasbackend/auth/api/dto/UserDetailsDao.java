package edward.com.finanzasbackend.auth.api.dto;

import edward.com.finanzasbackend.auth.domain.UserStatus;

public record UserDetailsDao(
        String name,
        String email,
        UserStatus status
) {
}
