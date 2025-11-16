package nl.rabobank.mapper;

import static nl.rabobank.account.AccountType.PAYMENT;
import static nl.rabobank.account.AccountType.SAVINGS;

import java.util.Optional;
import nl.rabobank.account.Account;
import nl.rabobank.account.AccountType;
import nl.rabobank.account.PaymentAccount;
import nl.rabobank.account.SavingsAccount;
import nl.rabobank.dto.AccountRequest;
import nl.rabobank.dto.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountApiMapper {

    public Account toDomain(AccountRequest request) {
        // Ensure balance if not provided in request
        var initialBalance = Optional.ofNullable(request.getInitialBalance()).orElse(0.0d);

        return switch (AccountType.valueOf(request.getAccountType())) {
            case PAYMENT ->
                new PaymentAccount(request.getAccountNumber(), request.getAccountHolderName(), initialBalance);
            case SAVINGS ->
                new SavingsAccount(request.getAccountNumber(), request.getAccountHolderName(), initialBalance);
        };
    }

    public AccountResponse toResponse(Account account) {
        var accountType =
                switch (account) {
                    case PaymentAccount p -> PAYMENT;
                    case SavingsAccount s -> SAVINGS;
                };

        return AccountResponse.builder()
                .accountNumber(account.accountNumber())
                .accountHolderName(account.accountHolderName())
                .balance(account.balance().toString())
                .accountType(String.valueOf(accountType))
                .build();
    }
}
