package nl.rabobank.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import nl.rabobank.account.Account;
import nl.rabobank.account.AccountType;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.apimapper.AccountApiMapper;
import nl.rabobank.apimapper.PowerOfAttorneyApiMapper;
import nl.rabobank.authorizations.Authorization;
import nl.rabobank.authorizations.PowerOfAttorney;
import nl.rabobank.dto.AccountResponse;
import nl.rabobank.dto.PowerOfAttorneyRequest;
import nl.rabobank.dto.PowerOfAttorneyResponse;
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
    void grantAccess_shouldReturn403_forGrantorNotAccountHolder() throws Exception {
        var account = getAccount("NL01TEST", "Alice", 100.0, AccountType.valueOf("PAYMENT"));

        var request = PowerOfAttorneyRequest.builder()
                .grantorName("Peter")
                .granteeName("Bob")
                .accountNumber("NL01TEST")
                .accountType("PAYMENT")
                .authorization("READ")
                .build();

        when(accountService.existsByAccountNumber(any())).thenReturn(true);
        when(accountService.getByAccountNumber("NL01TEST")).thenReturn(account);

        mockMvc.perform(post("/api/v1/power-of-attorney")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("The grantor Peter is not the accountHolder for account NL01TEST"));
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

    @Test
    void listByGrantee_shouldReturn200_withList() throws Exception {
        var account = new PaymentAccount("NL1", "Alice", 100.0);

        var poa1 = PowerOfAttorney.builder()
                .grantorName("Alice")
                .granteeName("Bob")
                .authorization(Authorization.READ)
                .account(account)
                .build();

        var poa2 = PowerOfAttorney.builder()
                .grantorName("Charlie")
                .granteeName("Bob")
                .authorization(Authorization.WRITE)
                .account(account)
                .build();

        var response1 = PowerOfAttorneyResponse.builder()
                .id("1")
                .grantorName("Alice")
                .granteeName("Bob")
                .authorization("READ")
                .account(new AccountApiMapper().toResponse(account))
                .build();

        var response2 = PowerOfAttorneyResponse.builder()
                .id("2")
                .grantorName("Charlie")
                .granteeName("Bob")
                .authorization("WRITE")
                .account(new AccountApiMapper().toResponse(account))
                .build();

        when(powerOfAttorneyService.findByGranteeName("Bob")).thenReturn(List.of(poa1, poa2));

        when(powerOfAttorneyApiMapper.toResponse(poa1)).thenReturn(response1);
        when(powerOfAttorneyApiMapper.toResponse(poa2)).thenReturn(response2);

        mockMvc.perform(get("/api/v1/power-of-attorney")
                        .param("granteeName", "Bob")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].granteeName").value("Bob"))
                .andExpect(jsonPath("$[0].authorization").value("READ"))
                .andExpect(jsonPath("$[1].authorization").value("WRITE"));

        verify(powerOfAttorneyService).findByGranteeName("Bob");
    }

    @Test
    void listByGrantee_shouldReturn200_withEmptyList() throws Exception {
        when(powerOfAttorneyService.findByGranteeName("Nobody")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/power-of-attorney").param("granteeName", "Nobody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(powerOfAttorneyService).findByGranteeName("Nobody");
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
