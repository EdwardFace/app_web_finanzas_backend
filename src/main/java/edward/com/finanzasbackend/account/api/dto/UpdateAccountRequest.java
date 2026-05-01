package edward.com.finanzasbackend.account.api.dto;

import edward.com.finanzasbackend.account.domain.AccountType;

public record UpdateAccountRequest(
        String name,
        AccountType type
) {}
