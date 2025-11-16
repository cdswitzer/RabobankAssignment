package nl.rabobank.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rabobank.account.Account;
import nl.rabobank.account.AccountType;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.authorizations.Authorization;
import nl.rabobank.authorizations.PowerOfAttorney;
import nl.rabobank.dto.AccountResponse;
import nl.rabobank.dto.PowerOfAttorneyRequest;
import nl.rabobank.dto.PowerOfAttorneyResponse;
import nl.rabobank.mapper.AccountApiMapper;
import nl.rabobank.mapper.PowerOfAttorneyApiMapper;
import nl.rabobank.service.AccountService;
import nl.rabobank.service.PowerOfAttorneyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PowerOfAttorneyController.class)
class PowerOfAttorneyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PowerOfAttorneyService powerOfAttorneyService;

    @MockitoBean
    private AccountService accountService;

    @Autowired
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PowerOfAttorneyApiMapper powerOfAttorneyApiMapper;

    @ParameterizedTest
    @CsvSource({"READ", "WRITE"})
    void grantAccess_shouldReturn201_forAuthorization(String authorization) throws Exception {
        var request = PowerOfAttorneyRequest.builder()
                .grantorName("Alice")
                .granteeName("Bob")
                .accountNumber("NL01TEST")
                .accountType("PAYMENT")
                .authorization(authorization)
                .build();

        var account = getAccount("NL01TEST", "Alice", 100.0, AccountType.valueOf("PAYMENT"));

        var powerOfAttorney = PowerOfAttorney.builder()
                .grantorName("Alice")
                .granteeName("Bob")
                .account(account)
                .authorization(Authorization.valueOf(authorization))
                .build();

        var powerOfAttorneyResponse = PowerOfAttorneyResponse.builder()
                .id("random_id")
                .grantorName("Alice")
                .granteeName("Bob")
                .authorization(authorization)
                .account(getAccountResponse(account))
                .build();

        when(accountService.existsByAccountNumber(any())).thenReturn(true);
        when(powerOfAttorneyService.grantAccess(any())).thenReturn(powerOfAttorney);
        when(powerOfAttorneyApiMapper.toResponse(powerOfAttorney)).thenReturn(powerOfAttorneyResponse);

        mockMvc.perform(post("/api/v1/power-of-attorney")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(powerOfAttorneyResponse)));
    }

    @ParameterizedTest
    @CsvSource({"READ", "WRITE"})
    void grantAccess_shouldReturn404_forNonExistingAccount(String authorization) throws Exception {
        var request = PowerOfAttorneyRequest.builder()
                .grantorName("Alice")
                .granteeName("Bob")
                .accountNumber("NL01TEST")
                .accountType("PAYMENT")
                .authorization(authorization)
                .build();

        when(accountService.existsByAccountNumber(any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/power-of-attorney")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No account found with number: NL01TEST"));
    }

    @Test
    void grantAccess_shouldReturn400_forMissingFields() throws Exception {
        var request = PowerOfAttorneyRequest.builder()
                .grantorName("Bart")
                .accountNumber("NL123456")
                .build();

        mockMvc.perform(post("/api/v1/power-of-attorney")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "{authorization=authorization is required, accountType=accountType is required, granteeName=granteeName is required}"));
    }

    private Account getAccount(
            String accountNumber, String accountHolderName, Double balance, AccountType accountType) {
        return switch (accountType) {
            case PAYMENT -> new PaymentAccount(accountNumber, accountHolderName, balance);
            case SAVINGS -> new SavingsAccount(accountNumber, accountHolderName, balance);
        };
    }

    private AccountResponse getAccountResponse(Account account) {
        return new AccountApiMapper().toResponse(account);
    }
}
