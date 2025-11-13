package nl.rabobank.dto;

import lombok.Builder;
import lombok.Data;
import nl.rabobank.account.AccountType;
import nl.rabobank.authorizations.Authorization;

@Data
@Builder
public class PowerOfAttorneyRequest {
    private String granteeName;
    private String grantorName;
    private Authorization authorization;
    private String accountNumber;
    private AccountType accountType;
}
