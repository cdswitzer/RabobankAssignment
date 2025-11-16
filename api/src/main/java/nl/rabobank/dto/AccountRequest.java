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
public class AccountRequest {

    @NotBlank(message = "accountNumber is required")
    private String accountNumber;

    @NotBlank(message = "accountHolderName is required")
    private String accountHolderName;

    @NotBlank(message = "accountType is required")
    private String accountType;

    private Double initialBalance;
}
