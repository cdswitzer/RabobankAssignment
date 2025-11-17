package nl.rabobank.service;

import static nl.rabobank.account.AccountType.PAYMENT;
import static nl.rabobank.authorizations.Authorization.READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import nl.rabobank.account.Account;
import nl.rabobank.account.AccountType;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.apimapper.PowerOfAttorneyApiMapper;
import nl.rabobank.authorizations.Authorization;
import nl.rabobank.authorizations.PowerOfAttorney;
import nl.rabobank.document.AccountDocument;
import nl.rabobank.document.PowerOfAttorneyDocument;
import nl.rabobank.dto.PowerOfAttorneyRequest;
import nl.rabobank.exception.AccountNotFoundException;
import nl.rabobank.mapper.AccountMapper;
import nl.rabobank.mapper.PowerOfAttorneyMapper;
import nl.rabobank.repository.AccountRepository;
import nl.rabobank.repository.PowerOfAttorneyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PowerOfAttorneyService Tests")
class PowerOfAttorneyServiceTest {

    @Mock
    private PowerOfAttorneyRepository powerOfAttorneyRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PowerOfAttorneyApiMapper powerOfAttorneyApiMapper;

    @Mock
    private PowerOfAttorneyMapper powerOfAttorneyMapper;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private PowerOfAttorneyService powerOfAttorneyService;

    @ParameterizedTest
    @CsvSource({"READ", "WRITE"})
    void grantAccess_shouldReturnPowerOfAttorney_forAuthorization(String authorization) {
        var granteeName = "Alice";
        var grantorName = "Bob";
        var accountNumber = "NL123456";

        var request = PowerOfAttorneyRequest.builder()
                .granteeName(granteeName)
                .grantorName(grantorName)
                .authorization(authorization)
                .accountNumber(accountNumber)
                .accountType(String.valueOf(PAYMENT))
                .build();

        var account = getAccount(accountNumber, grantorName, 1000.0, PAYMENT);
        var accountDocument = getAccountDocument(account);

        var savedDocument = PowerOfAttorneyDocument.builder()
                .id("poa-123")
                .granteeName(granteeName)
                .grantorName(grantorName)
                .accountDocument(accountDocument)
                .authorization(Authorization.valueOf(authorization))
                .build();

        var powerOfAttorney = PowerOfAttorney.builder()
                .granteeName(granteeName)
                .grantorName(grantorName)
                .authorization(Authorization.valueOf(authorization))
                .account(account)
                .build();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(accountDocument));
        when(accountMapper.toDomain(accountDocument)).thenReturn(account);
        when(powerOfAttorneyApiMapper.toDomain(request, account)).thenReturn(powerOfAttorney);
        when(powerOfAttorneyMapper.toDocument(powerOfAttorney)).thenReturn(savedDocument);
        when(powerOfAttorneyRepository.save(savedDocument)).thenReturn(savedDocument);
        when(powerOfAttorneyMapper.toDomain(savedDocument)).thenReturn(powerOfAttorney);

        PowerOfAttorney result = powerOfAttorneyService.grantAccess(request);

        assertThat(result).isNotNull().satisfies(poa -> {
            assertThat(poa.granteeName()).isEqualTo(granteeName);
            assertThat(poa.grantorName()).isEqualTo(grantorName);
            assertThat(poa.authorization()).isEqualTo(Authorization.valueOf(authorization));
            assertThat(poa.account()).isNotNull();
            assertThat(poa.account().accountNumber()).isEqualTo(accountNumber);
        });

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(powerOfAttorneyRepository).save(any(PowerOfAttorneyDocument.class));
    }

    @Test
    void findByAccountNumber_shouldThrowException_forNonExistingAccount() {
        PowerOfAttorneyRequest request = PowerOfAttorneyRequest.builder()
                .granteeName("Alice")
                .grantorName("Bob")
                .authorization(String.valueOf(READ))
                .accountNumber("NL999999")
                .accountType(String.valueOf(PAYMENT))
                .build();

        when(accountRepository.findByAccountNumber("NL999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> powerOfAttorneyService.grantAccess(request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("No account found with number: NL999999");

        verify(accountRepository).findByAccountNumber("NL999999");
        verify(powerOfAttorneyRepository, never()).save(any(PowerOfAttorneyDocument.class));
    }

    @Test
    void findByGranteeName_shouldReturnList_forGranteeName() {
        String grantee = "Alice";

        var account = new PaymentAccount("NL111", "Bob", 100.0);
        var accountDocument = new AccountMapper().toDocument(account);

        var doc1 = PowerOfAttorneyDocument.builder()
                .id("poa-1")
                .granteeName(grantee)
                .grantorName("Bob")
                .authorization(Authorization.READ)
                .accountDocument(accountDocument)
                .build();

        var doc2 = PowerOfAttorneyDocument.builder()
                .id("poa-2")
                .granteeName(grantee)
                .grantorName("Charlie")
                .authorization(Authorization.WRITE)
                .accountDocument(accountDocument)
                .build();

        var poa1 = PowerOfAttorney.builder()
                .granteeName(grantee)
                .grantorName("Bob")
                .authorization(Authorization.READ)
                .account(account)
                .build();

        var poa2 = PowerOfAttorney.builder()
                .granteeName(grantee)
                .grantorName("Charlie")
                .authorization(Authorization.WRITE)
                .account(account)
                .build();

        when(powerOfAttorneyRepository.findByGranteeName(grantee)).thenReturn(List.of(doc1, doc2));
        when(powerOfAttorneyMapper.toDomain(doc1)).thenReturn(poa1);
        when(powerOfAttorneyMapper.toDomain(doc2)).thenReturn(poa2);

        var result = powerOfAttorneyService.findByGranteeName(grantee);

        assertThat(result).hasSize(2).containsExactly(poa1, poa2);

        verify(powerOfAttorneyRepository).findByGranteeName(grantee);
        verify(powerOfAttorneyMapper).toDomain(doc1);
        verify(powerOfAttorneyMapper).toDomain(doc2);
    }

    @Test
    void findByGranteeName_shouldReturnEmptyList_whenNoResults() {
        when(powerOfAttorneyRepository.findByGranteeName("Unknown")).thenReturn(List.of());

        var result = powerOfAttorneyService.findByGranteeName("Unknown");

        assertThat(result).isEmpty();

        verify(powerOfAttorneyRepository).findByGranteeName("Unknown");
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
