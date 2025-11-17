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
import nl.rabobank.account.AccountType;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.apimapper.AccountApiMapper;
import nl.rabobank.dto.AccountRequest;
import nl.rabobank.dto.AccountResponse;
import nl.rabobank.exception.AccountNotFoundException;
import nl.rabobank.exception.DuplicateAccountException;
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

        verify(accountService).createAccount(any(AccountRequest.class));
        verify(accountApiMapper).toResponse(account);
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

        verify(accountService).createAccount(any(AccountRequest.class));
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

    @Test
    void getByAccountNumber_shouldReturn200_withAccount() throws Exception {
        var account = new PaymentAccount("NL123456", "John Doe", 1000.0);

        var response = AccountResponse.builder()
                .accountNumber("NL123456")
                .accountHolderName("John Doe")
                .balance("1000.0")
                .accountType("PAYMENT")
                .build();

        when(accountService.getByAccountNumber("NL123456")).thenReturn(account);
        when(accountApiMapper.toResponse(account)).thenReturn(response);

        mockMvc.perform(get("/api/v1/accounts/NL123456").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        verify(accountService).getByAccountNumber("NL123456");
        verify(accountApiMapper).toResponse(account);
    }

    @Test
    void getByAccountNumber_shouldReturn404_whenNotFound() throws Exception {
        when(accountService.getByAccountNumber("NL000000"))
                .thenThrow(new AccountNotFoundException("Account with number 'NL000000' not found"));

        mockMvc.perform(get("/api/v1/accounts/NL000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Account with number 'NL000000' not found"));

        verify(accountService).getByAccountNumber("NL000000");
    }

    @Test
    void listAll_shouldReturn200_withListOfAccounts() throws Exception {
        var acc1 = new PaymentAccount("NL1", "Alice", 100.0);
        var acc2 = new SavingsAccount("NL2", "Bob", 200.0);

        var response1 = AccountResponse.builder()
                .accountNumber("NL1")
                .accountHolderName("Alice")
                .balance("100.0")
                .accountType("PAYMENT")
                .build();
        var response2 = AccountResponse.builder()
                .accountNumber("NL2")
                .accountHolderName("Bob")
                .balance("200.0")
                .accountType("SAVINGS")
                .build();

        when(accountService.findAll()).thenReturn(List.of(acc1, acc2));
        when(accountApiMapper.toResponse(acc1)).thenReturn(response1);
        when(accountApiMapper.toResponse(acc2)).thenReturn(response2);

        mockMvc.perform(get("/api/v1/accounts").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountNumber").value("NL1"))
                .andExpect(jsonPath("$[1].accountNumber").value("NL2"));

        verify(accountService).findAll();
    }
}
