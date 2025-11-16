package nl.rabobank.service;

import nl.rabobank.account.Account;
import nl.rabobank.account.AccountType;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.document.AccountDocument;
import nl.rabobank.dto.AccountRequest;
import nl.rabobank.exception.DuplicateAccountException;
import nl.rabobank.apimapper.AccountApiMapper;
import nl.rabobank.mapper.AccountMapper;
import nl.rabobank.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static nl.rabobank.account.AccountType.PAYMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountApiMapper accountApiMapper;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvSource({
            "NL123456, John Doe, 1000.0, PAYMENT, Should create payment account with positive balance",
            "NL654321, Mary Doe, 500.0, SAVINGS, Should create savings account with positive balance",
            "NL456789, John Doe, -1000.0, SAVINGS, Should create payment account with negative balance",
            "NL987654, Mary Doe, -500.0, SAVINGS, Should create savings account with negative balance"
    })
    void createAccount_shouldCreateNewAccount_forRequest(String accountNumber, String accountHolderName, Double balance, String accountType, String testName) {
        var request = AccountRequest.builder()
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .initialBalance(balance)
                .accountType(accountType)
                .build();

        var account = getAccount(accountNumber, accountHolderName, balance, AccountType.valueOf(accountType));
        var savedDocument = getAccountDocument(account);

        when(accountRepository.existsByAccountNumber(accountNumber)).thenReturn(false);
        when(accountApiMapper.toDomain(request)).thenReturn(account);
        when(accountMapper.toDocument(account)).thenReturn(savedDocument);
        when(accountRepository.save(any(AccountDocument.class))).thenReturn(savedDocument);
        when(accountMapper.toDomain(savedDocument)).thenReturn(account);

        var result = accountService.createAccount(request);

        var accountTypeClass =
                switch (AccountType.valueOf(accountType)) {
                    case PAYMENT -> PaymentAccount.class;
                    case SAVINGS -> SavingsAccount.class;
                };

        assertThat(result)
                .isNotNull()
                .isInstanceOf(accountTypeClass)
                .satisfies(acc -> {
                    assertThat(acc.accountNumber()).isEqualTo(accountNumber);
                    assertThat(acc.accountHolderName()).isEqualTo(accountHolderName);
                    assertThat(acc.balance()).isEqualTo(balance);
                });

        verify(accountRepository).existsByAccountNumber(accountNumber);
        verify(accountRepository).save(any(AccountDocument.class));
    }

    @Test
    void createAccount_shouldThrowException_forExistingAccount() {
        var request = AccountRequest.builder()
                .accountNumber("NL123456")
                .accountHolderName("John Doe")
                .initialBalance(1000.0)
                .accountType(String.valueOf(AccountType.PAYMENT))
                .build();

        when(accountRepository.existsByAccountNumber("NL123456")).thenReturn(true);

        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessage("Account already exists with number: NL123456");

        verify(accountRepository).existsByAccountNumber("NL123456");
        verify(accountRepository, never()).save(any(AccountDocument.class));
    }

    @Test
    void getAccountByNumber_shouldReturnAccount_forAccountNumber() {
        var document = AccountDocument.builder()
                .accountNumber("NL123456")
                .accountHolderName("John Doe")
                .balance(1000.0)
                .accountType(PAYMENT)
                .build();

        var account = getAccount("NL123456", "John Doe", 1000.0, PAYMENT);
        when(accountRepository.findByAccountNumber("NL123456")).thenReturn(Optional.of(document));
        when(accountMapper.toDomain(document)).thenReturn(account);

        Optional<Account> result = accountService.getAccountByNumber("NL123456");

        assertThat(result)
                .isPresent()
                .get()
                .satisfies(acc -> {
                    assertThat(acc.accountNumber()).isEqualTo("NL123456");
                    assertThat(acc.accountHolderName()).isEqualTo("John Doe");
                    assertThat(acc.balance()).isEqualTo(1000.0);
                });

        verify(accountRepository).findByAccountNumber("NL123456");
    }

    @Test
    void getAccountByNumber_shouldReturnOptionalEmpty_forNonExistingAccountNumber() {
        when(accountRepository.findByAccountNumber("NL123456")).thenReturn(Optional.empty());

        Optional<Account> result = accountService.getAccountByNumber("NL123456");

        assertThat(result).isEmpty();
        verify(accountRepository).findByAccountNumber("NL123456");
    }

    @Test
    void existsByAccountNumber_shouldReturnTrue_whenAccountExists() {
        when(accountRepository.existsByAccountNumber("NL123456")).thenReturn(true);

        boolean result = accountService.existsByAccountNumber("NL123456");

        assertThat(result).isTrue();
        verify(accountRepository).existsByAccountNumber("NL123456");
    }

    @Test
    void existsByAccountNumber_shouldReturnFalse_whenAccountDoesNotExists() {
        when(accountRepository.existsByAccountNumber("NL123456")).thenReturn(false);

        boolean result = accountService.existsByAccountNumber("NL123456");

        assertThat(result).isFalse();
        verify(accountRepository).existsByAccountNumber("NL123456");
    }

    private Account getAccount(
            String accountNumber, String accountHolderName, Double balance, AccountType accountType) {
        return switch (accountType) {
            case PAYMENT -> new PaymentAccount(accountNumber, accountHolderName, balance);
            case SAVINGS -> new SavingsAccount(accountNumber, accountHolderName, balance);
        };
    }

    private AccountDocument getAccountDocument(Account account) {
        return new AccountMapper().toDocument(account);
    }
}