package nl.rabobank.account;

public sealed interface Account permits PaymentAccount, SavingsAccount {
    String accountNumber();

    String accountHolderName();

    Double balance();
}
