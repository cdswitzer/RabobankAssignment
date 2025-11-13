package nl.rabobank.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PowerOfAttorneyResponse {
    private String id;
    private String granteeName;
    private String grantorName;
    private String authorization;
    private AccountResponse account;
}
