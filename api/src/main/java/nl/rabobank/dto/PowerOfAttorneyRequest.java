package nl.rabobank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerOfAttorneyRequest {

    @NotBlank(message = "grantorName is required")
    private String grantorName;

    @NotBlank(message = "granteeName is required")
    private String granteeName;

    @NotBlank(message = "accountNumber is required")
    private String accountNumber;

    @NotBlank(message = "accountType is required")
    private String accountType;

    @NotBlank(message = "authorization is required")
    private String authorization;
}
