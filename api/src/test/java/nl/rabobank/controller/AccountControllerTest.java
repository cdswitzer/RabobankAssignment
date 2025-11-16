package nl.rabobank.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.rabobank.account.AccountType;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.dto.AccountRequest;
import nl.rabobank.dto.AccountResponse;
import nl.rabobank.exception.DuplicateAccountException;
import nl.rabobank.mapper.AccountApiMapper;
import nl.rabobank.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AccountApiMapper accountApiMapper;

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvSource({
        "NL123456, John Doe, 1000.0, PAYMENT, Should create payment account with positive balance",
        "NL654321, Mary Doe, 500.0, SAVINGS, Should create savings account with positive balance",
        "NL456789, John Doe, -1000.0, SAVINGS, Should create payment account with negative balance",
        "NL987654, Mary Doe, -500.0, SAVINGS, Should create savings account with negative balance"
    })
    void create_shouldCreateAccount_forAccountRequest(
            String accountNumber, String accountHolderName, Double balance, String accountType, String testName)
            throws Exception {
        var request = AccountRequest.builder()
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .initialBalance(balance)
                .accountType(accountType)
                .build();

        var account =
                switch (AccountType.valueOf(accountType)) {
                    case PAYMENT -> new PaymentAccount(accountNumber, accountHolderName, balance);
                    case SAVINGS -> new SavingsAccount(accountNumber, accountHolderName, balance);
                };

        var response = AccountResponse.builder()
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .balance(balance.toString())
                .accountType(accountType)
                .build();

        when(accountService.createAccount(any(AccountRequest.class))).thenReturn(account);
        when(accountApiMapper.toResponse(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accountNumber").value(accountNumber))
                .andExpect(jsonPath("$.accountHolderName").value(accountHolderName))
                .andExpect(jsonPath("$.balance").value(balance.toString()))
                .andExpect(jsonPath("$.accountType").value(accountType));

        verify(accountService, times(1)).createAccount(any(AccountRequest.class));
        verify(accountApiMapper, times(1)).toResponse(account);
    }

    @Test
    void create_shouldReturn409_whenAccountExists() throws Exception {
        var request = AccountRequest.builder()
                .accountNumber("NL123456")
                .accountHolderName("John Doe")
                .initialBalance(1000.0)
                .accountType("PAYMENT")
                .build();

        when(accountService.createAccount(any(AccountRequest.class)))
                .thenThrow(new DuplicateAccountException("Account already exists with number: NL123456"));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Account already exists with number: NL123456"));

        verify(accountService, times(1)).createAccount(any(AccountRequest.class));
    }

    @Test
    void create_shouldReturn400_forMissingFields() throws Exception {
        var request = AccountRequest.builder()
                .accountNumber("NL123456")
                .initialBalance(1000.0)
                .build();

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "{accountType=accountType is required, accountHolderName=accountHolderName is required}"));
    }
}
