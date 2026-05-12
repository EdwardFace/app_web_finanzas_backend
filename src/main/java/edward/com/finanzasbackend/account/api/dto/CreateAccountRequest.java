package edward.com.finanzasbackend.account.api.dto;

import edward.com.finanzasbackend.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotBlank String name,
        @NotNull AccountType type
) {}
