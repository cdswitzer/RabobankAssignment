package nl.rabobank.apimapper;

import static org.assertj.core.api.Assertions.assertThat;

import nl.rabobank.account.AccountType;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.dto.AccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AccountApiMapperTest {

    private AccountApiMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AccountApiMapper();
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvSource({
        "NL123456, John Doe, 1000.0, PAYMENT, Payment account with positive balance",
        "NL654321, Mary Doe, 500.0, SAVINGS, Savings account with positive balance",
        "NL456789, John Doe, -1000.0, SAVINGS, Payment account with negative balance",
        "NL987654, Mary Doe, -500.0, SAVINGS, Savings account with negative balance"
    })
    void toDomain_shouldMap_fromRequest(
            String accountNumber, String accountHolderName, Double balance, String accountType, String testName) {
        var request = AccountRequest.builder()
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .initialBalance(balance)
                .accountType(accountType)
                .build();

        var account = mapper.toDomain(request);

        var accountTypeClass =
                switch (AccountType.valueOf(accountType)) {
                    case PAYMENT -> PaymentAccount.class;
                    case SAVINGS -> SavingsAccount.class;
                };

        assertThat(account).isNotNull().isInstanceOf(accountTypeClass).satisfies(acc -> {
            assertThat(acc.accountNumber()).isEqualTo(accountNumber);
            assertThat(acc.accountHolderName()).isEqualTo(accountHolderName);
            assertThat(acc.balance()).isEqualTo(balance);
        });
    }

    @ParameterizedTest
    @CsvSource({"PAYMENT", "SAVINGS"})
    void toDomain_shouldMap_fromRequestWithoutBalance(String accountType) {
        var request = AccountRequest.builder()
                .accountNumber("NL123456")
                .accountHolderName("John Doe")
                .accountType(accountType)
                .build();

        var account = mapper.toDomain(request);

        var accountTypeClass =
                switch (AccountType.valueOf(accountType)) {
                    case PAYMENT -> PaymentAccount.class;
                    case SAVINGS -> SavingsAccount.class;
                };

        assertThat(account).isNotNull().isInstanceOf(accountTypeClass).satisfies(acc -> {
            assertThat(acc.accountNumber()).isEqualTo("NL123456");
            assertThat(acc.accountHolderName()).isEqualTo("John Doe");
            assertThat(acc.balance()).isEqualTo(0.0);
        });
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvSource({
        "NL123456, John Doe, 1000.0, PAYMENT, Payment document with positive balance",
        "NL654321, Mary Doe, 500.0, SAVINGS, Savings document with positive balance",
        "NL456789, John Doe, -1000.0, SAVINGS, Payment document with negative balance",
        "NL987654, Mary Doe, -500.0, SAVINGS, Savings document with negative balance"
    })
    void toResponse_shouldMap_fromAccount(
            String accountNumber, String accountHolderName, Double balance, AccountType accountType, String testName) {
        var account =
                switch (accountType) {
                    case PAYMENT -> new PaymentAccount(accountNumber, accountHolderName, balance);
                    case SAVINGS -> new SavingsAccount(accountNumber, accountHolderName, balance);
                };

        var document = mapper.toResponse(account);

        assertThat(document).isNotNull().satisfies(doc -> {
            assertThat(doc.getAccountNumber()).isEqualTo(accountNumber);
            assertThat(doc.getAccountHolderName()).isEqualTo(accountHolderName);
            assertThat(doc.getBalance()).isEqualTo(balance.toString());
            assertThat(doc.getAccountType()).isEqualTo(accountType.toString());
        });
    }
}
