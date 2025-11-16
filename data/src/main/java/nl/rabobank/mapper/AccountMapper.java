package nl.rabobank.mapper;

import static nl.rabobank.account.AccountType.PAYMENT;
import static nl.rabobank.account.AccountType.SAVINGS;

import nl.rabobank.account.Account;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.document.AccountDocument;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountDocument toDocument(Account account) {
        return switch (account) {
            case PaymentAccount(String accountNumber, String accountHolderName, Double balance) ->
                AccountDocument.builder()
                        .accountType(PAYMENT)
                        .accountNumber(accountNumber)
                        .accountHolderName(accountHolderName)
                        .balance(balance)
                        .build();
            case SavingsAccount(String accountNumber, String accountHolderName, Double balance) ->
                AccountDocument.builder()
                        .accountType(SAVINGS)
                        .accountNumber(accountNumber)
                        .accountHolderName(accountHolderName)
                        .balance(balance)
                        .build();
            default -> throw new IllegalArgumentException("Unknown Account type: " + account.getClass());
        };
    }

    public Account toDomain(AccountDocument document) {
        return switch (document.getAccountType()) {
            case PAYMENT ->
                new PaymentAccount(document.getAccountNumber(), document.getAccountHolderName(), document.getBalance());
            case SAVINGS ->
                new SavingsAccount(document.getAccountNumber(), document.getAccountHolderName(), document.getBalance());
        };
    }
}
