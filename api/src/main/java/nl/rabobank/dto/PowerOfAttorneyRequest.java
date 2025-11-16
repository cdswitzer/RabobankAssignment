package nl.rabobank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.rabobank.account.AccountType;
import nl.rabobank.authorizations.Authorization;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerOfAttorneyRequest {
    private String granteeName;
    private String grantorName;
    private Authorization authorization;
    private String accountNumber;
    private AccountType accountType;
}
