package edward.com.finanzasbackend.account.api.dto;

import edward.com.finanzasbackend.account.domain.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String name,
        AccountType type,
        BigDecimal balance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
