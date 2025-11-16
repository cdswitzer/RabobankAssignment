package nl.rabobank.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import nl.rabobank.account.Account;
import nl.rabobank.account.AccountType;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.authorizations.Authorization;
import nl.rabobank.authorizations.PowerOfAttorney;
import nl.rabobank.dto.PowerOfAttorneyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PowerOfAttorneyApiMapperTest {

    private PowerOfAttorneyApiMapper mapper;
    private AccountApiMapper accountApiMapper;

    @BeforeEach
    void setUp() {
        accountApiMapper = new AccountApiMapper();
        mapper = new PowerOfAttorneyApiMapper(accountApiMapper);
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @CsvSource({
        "John Doe, Frank Bank, READ, NL123456, 1000.0, PAYMENT, Grant READ",
        "Mary Doe, Pieter Post, WRITE, NL654321, 500.0, SAVINGS, Grant WRITE",
    })
    void toDomain_shouldMap_fromPowerOfAttorneyRequest(
            String grantorName,
            String granteeName,
            String authorization,
            String accountNumber,
            Double balance,
            String accountType,
            String testName) {

        var account = getAccount(accountNumber, grantorName, balance, AccountType.valueOf(accountType));
        var powerOfAttorneyRequest = PowerOfAttorneyRequest.builder()
                .grantorName(grantorName)
                .granteeName(granteeName)
                .authorization(authorization)
                .accountNumber(accountNumber)
                .accountType(accountType)
                .build();

        var powerOfAttorney = mapper.toDomain(powerOfAttorneyRequest, account);

        assertThat(powerOfAttorney).isNotNull().satisfies(poa -> {
            assertThat(poa.granteeName()).isEqualTo(granteeName);
            assertThat(poa.grantorName()).isEqualTo(grantorName);
            assertThat(poa.account()).isEqualTo(account);
            assertThat(poa.authorization().toString()).isEqualTo(authorization);
        });
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @CsvSource({
        "John Doe, Frank Bank, READ, NL123456, 1000.0, PAYMENT, Grant READ",
        "Mary Doe, Pieter Post, WRITE, NL654321, 500.0, SAVINGS, Grant WRITE",
    })
    void toResponse_shouldMap_fromPowerOfAttorney(
            String grantorName,
            String granteeName,
            String authorization,
            String accountNumber,
            Double balance,
            String accountType,
            String testName) {
        var account = getAccount(accountNumber, grantorName, balance, AccountType.valueOf(accountType));

        var powerOfAttorney = PowerOfAttorney.builder()
                .granteeName(granteeName)
                .grantorName(grantorName)
                .account(account)
                .authorization(Authorization.valueOf(authorization))
                .build();

        var document = mapper.toResponse(powerOfAttorney);

        assertThat(document).isNotNull().satisfies(doc -> {
            assertThat(doc.getGranteeName()).isEqualTo(granteeName);
            assertThat(doc.getGrantorName()).isEqualTo(grantorName);
            assertThat(doc.getAuthorization()).isEqualTo(authorization);
            assertThat(doc.getAccount()).isEqualTo(accountApiMapper.toResponse(account));
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
