package nl.rabobank.dto;

import lombok.Builder;
import lombok.Data;
import nl.rabobank.account.AccountType;

@Data
@Builder
public class AccountRequest {
    private String accountNumber;
    private String accountHolderName;
    private Double initialBalance;
    private AccountType accountType;
}
