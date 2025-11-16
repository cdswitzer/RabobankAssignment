package nl.rabobank.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import nl.rabobank.account.AccountType;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.document.AccountDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AccountMapperTest {

    private AccountMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AccountMapper();
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvSource({
        "NL123456, John Doe, 1000.0, PAYMENT, Payment account with positive balance",
        "NL654321, Mary Doe, 500.0, SAVINGS, Savings account with positive balance",
        "NL456789, John Doe, -1000.0, SAVINGS, Payment account with negative balance",
        "NL987654, Mary Doe, -500.0, SAVINGS, Savings account with negative balance"
    })
    void toDocument_shouldMap_fromAccount(
            String accountNumber, String accountHolderName, Double balance, AccountType accountType, String testName) {
        var account =
                switch (accountType) {
                    case PAYMENT -> new PaymentAccount(accountNumber, accountHolderName, balance);
                    case SAVINGS -> new SavingsAccount(accountNumber, accountHolderName, balance);
                };

        var document = mapper.toDocument(account);

        assertThat(document).isNotNull().satisfies(doc -> {
            assertThat(doc.getAccountNumber()).isEqualTo(accountNumber);
            assertThat(doc.getAccountHolderName()).isEqualTo(accountHolderName);
            assertThat(doc.getBalance()).isEqualTo(balance);
            assertThat(doc.getAccountType()).isEqualTo(accountType);
        });
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvSource({
        "NL123456, John Doe, 1000.0, PAYMENT, Payment document with positive balance",
        "NL654321, Mary Doe, 500.0, SAVINGS, Savings document with positive balance",
        "NL456789, John Doe, -1000.0, SAVINGS, Payment document with negative balance",
        "NL987654, Mary Doe, -500.0, SAVINGS, Savings document with negative balance"
    })
    void toDomain_shouldMap_fromDocument(
            String accountNumber, String accountHolderName, Double balance, AccountType accountType, String testName) {
        var document = AccountDocument.builder()
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .balance(balance)
                .accountType(accountType)
                .build();

        var account = mapper.toDomain(document);

        var accountTypeClass =
                switch (accountType) {
                    case PAYMENT -> PaymentAccount.class;
                    case SAVINGS -> SavingsAccount.class;
                };

        assertThat(account).isNotNull().isInstanceOf(accountTypeClass).satisfies(acc -> {
            assertThat(acc.accountNumber()).isEqualTo(accountNumber);
            assertThat(acc.accountHolderName()).isEqualTo(accountHolderName);
            assertThat(acc.balance()).isEqualTo(balance);
        });
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvSource({
        "NL123456, John Doe, 1000.0, PAYMENT, Payment account to document to account",
        "NL654321, Mary Doe, 500.0, SAVINGS, Savings account to document to account"
    })
    void mapper_shouldReturnSameValue_forAccountToDocumentToAccount(
            String accountNumber, String accountHolderName, Double balance, AccountType accountType, String testName) {
        var account =
                switch (accountType) {
                    case PAYMENT -> new PaymentAccount(accountNumber, accountHolderName, balance);
                    case SAVINGS -> new SavingsAccount(accountNumber, accountHolderName, balance);
                };

        var document = mapper.toDocument(account);
        var convertedAccount = mapper.toDomain(document);

        assertThat(convertedAccount).isEqualTo(account);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @CsvSource({
        "NL123456, John Doe, 1000.0, PAYMENT, Document to payment account to document",
        "NL654321, Mary Doe, 500.0, SAVINGS, Document to savings account to document"
    })
    void mapper_shouldReturnSameValue_forDocumentToAccountToDocument(
            String accountNumber, String accountHolderName, Double balance, AccountType accountType, String testName) {
        var document = AccountDocument.builder()
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .balance(balance)
                .accountType(accountType)
                .build();

        var account = mapper.toDomain(document);
        var convertedDocument = mapper.toDocument(account);

        assertThat(convertedDocument).isEqualTo(document);
    }
}
