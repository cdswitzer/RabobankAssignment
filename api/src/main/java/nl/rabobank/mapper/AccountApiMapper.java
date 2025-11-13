package nl.rabobank.mapper;

import static nl.rabobank.account.AccountType.PAYMENT;
import static nl.rabobank.account.AccountType.SAVINGS;

import nl.rabobank.account.Account;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.dto.AccountRequest;
import nl.rabobank.dto.AccountResponse;

public class AccountApiMapper {

    public static Account toDomain(AccountRequest request) {
        return switch (request.getAccountType()) {
            case PAYMENT ->
                new PaymentAccount(
                        request.getAccountNumber(), request.getAccountHolderName(), request.getInitialBalance());
            case SAVINGS ->
                new SavingsAccount(
                        request.getAccountNumber(), request.getAccountHolderName(), request.getInitialBalance());
        };
    }

    public static AccountResponse toResponse(Account account) {
        var accountType =
                switch (account) {
                    case PaymentAccount p -> PAYMENT;
                    case SavingsAccount s -> SAVINGS;
                    default -> throw new IllegalArgumentException("Unknown Account type");
                };

        return AccountResponse.builder()
                .accountNumber(account.accountNumber())
                .accountHolderName(account.accountHolderName())
                .balance(account.balance().toString())
                .accountType(String.valueOf(accountType))
                .build();
    }
}
