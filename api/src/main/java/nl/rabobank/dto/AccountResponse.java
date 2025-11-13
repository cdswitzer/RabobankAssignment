package nl.rabobank.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountResponse {
    private String accountNumber;
    private String accountHolderName;
    private String accountType;
    private String balance;
}
