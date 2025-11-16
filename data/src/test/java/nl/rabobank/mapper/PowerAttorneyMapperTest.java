package nl.rabobank.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import nl.rabobank.account.Account;
import nl.rabobank.account.AccountType;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.authorizations.Authorization;
import nl.rabobank.authorizations.PowerOfAttorney;
import nl.rabobank.document.PowerOfAttorneyDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PowerAttorneyMapperTest {

    private PowerOfAttorneyMapper mapper;
    private AccountMapper accountMapper;

    @BeforeEach
    void setUp() {
        accountMapper = new AccountMapper();
        mapper = new PowerOfAttorneyMapper(accountMapper);
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @CsvSource({
        "John Doe, Frank Bank, READ, NL123456, 1000.0, PAYMENT, Grant READ",
        "Mary Doe, Pieter Post, WRITE, NL654321, 500.0, SAVINGS, Grant WRITE",
    })
    void toDocument_shouldMap_fromPowerOfAttorney(
            String grantorName,
            String granteeName,
            Authorization authorization,
            String accountNumber,
            Double balance,
            AccountType accountType,
            String testName) {
        var account = getAccount(accountNumber, grantorName, balance, accountType);

        var powerOfAttorney = PowerOfAttorney.builder()
                .granteeName(granteeName)
                .grantorName(grantorName)
                .account(account)
                .authorization(authorization)
                .build();

        var document = mapper.toDocument(powerOfAttorney);

        assertThat(document).isNotNull().satisfies(doc -> {
            assertThat(doc.getGranteeName()).isEqualTo(granteeName);
            assertThat(doc.getGrantorName()).isEqualTo(grantorName);
            assertThat(doc.getAuthorization()).isEqualTo(authorization);
            assertThat(doc.getAccountDocument()).isEqualTo(accountMapper.toDocument(account));
        });
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @CsvSource({
        "John Doe, Frank Bank, READ, NL123456, 1000.0, PAYMENT, Grant READ",
        "Mary Doe, Pieter Post, WRITE, NL654321, 500.0, SAVINGS, Grant WRITE",
    })
    void toDomain_shouldMap_fromDocument(
            String grantorName,
            String granteeName,
            Authorization authorization,
            String accountNumber,
            Double balance,
            AccountType accountType,
            String testName) {

        var account = getAccount(accountNumber, grantorName, balance, accountType);
        var accountDocument = accountMapper.toDocument(account);

        var powerOfAttorneyDocument = PowerOfAttorneyDocument.builder()
                .granteeName(granteeName)
                .grantorName(grantorName)
                .accountDocument(accountDocument)
                .authorization(authorization)
                .build();

        var powerOfAttorney = mapper.toDomain(powerOfAttorneyDocument);

        assertThat(powerOfAttorney).isNotNull().satisfies(p -> {
            assertThat(p.granteeName()).isEqualTo(granteeName);
            assertThat(p.grantorName()).isEqualTo(grantorName);
            assertThat(p.account()).isEqualTo(account);
            assertThat(p.authorization()).isEqualTo(authorization);
        });
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @CsvSource({
        "John Doe, Frank Bank, READ, NL123456, 1000.0, PAYMENT, Grant READ",
        "Mary Doe, Pieter Post, WRITE, NL654321, 500.0, SAVINGS, Grant WRITE",
    })
    void mapper_shouldReturnSameValue_forPowerOfAttorneyToDocumentToPowerOfAttorney(
            String grantorName,
            String granteeName,
            Authorization authorization,
            String accountNumber,
            Double balance,
            AccountType accountType,
            String testName) {
        var account = getAccount(accountNumber, grantorName, balance, accountType);

        var powerOfAttorney = PowerOfAttorney.builder()
                .granteeName(granteeName)
                .grantorName(grantorName)
                .account(account)
                .authorization(authorization)
                .build();

        var document = mapper.toDocument(powerOfAttorney);
        var convertedPowerOfAttorney = mapper.toDomain(document);

        assertThat(convertedPowerOfAttorney).isNotNull().satisfies(p -> {
            assertThat(p.granteeName()).isEqualTo(granteeName);
            assertThat(p.grantorName()).isEqualTo(grantorName);
            assertThat(p.account()).isEqualTo(account);
            assertThat(p.authorization()).isEqualTo(authorization);
        });
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @CsvSource({
        "John Doe, Frank Bank, READ, NL123456, 1000.0, PAYMENT, Grant READ",
        "Mary Doe, Pieter Post, WRITE, NL654321, 500.0, SAVINGS, Grant WRITE",
    })
    void mapper_shouldReturnSameValue_forDocumentToPowerOfAttorneyToDcoument(
            String grantorName,
            String granteeName,
            Authorization authorization,
            String accountNumber,
            Double balance,
            AccountType accountType,
            String testName) {
        var account = getAccount(accountNumber, grantorName, balance, accountType);
        var accountDocument = accountMapper.toDocument(account);

        var powerOfAttorneyDocument = PowerOfAttorneyDocument.builder()
                .granteeName(granteeName)
                .grantorName(grantorName)
                .accountDocument(accountDocument)
                .authorization(authorization)
                .build();

        var powerOfAttorney = mapper.toDomain(powerOfAttorneyDocument);
        var convertedDocument = mapper.toDocument(powerOfAttorney);

        assertThat(convertedDocument).isNotNull().satisfies(doc -> {
            assertThat(doc.getGranteeName()).isEqualTo(granteeName);
            assertThat(doc.getGrantorName()).isEqualTo(grantorName);
            assertThat(doc.getAuthorization()).isEqualTo(authorization);
            assertThat(doc.getAccountDocument()).isEqualTo(accountMapper.toDocument(account));
        });
    }

    private Account getAccount(
            String accountNumber, String accountHolderName, Double balance, AccountType accountType) {
        return switch (accountType) {
            case PAYMENT -> new PaymentAccount(accountNumber, accountHolderName, balance);
            case SAVINGS -> new SavingsAccount(accountNumber, accountHolderName, balance);
        };
    }
}
