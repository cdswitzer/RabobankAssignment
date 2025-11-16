package nl.rabobank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.rabobank.account.AccountType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {
    private String accountNumber;
    private String accountHolderName;
    private Double initialBalance;
    private AccountType accountType;
}
