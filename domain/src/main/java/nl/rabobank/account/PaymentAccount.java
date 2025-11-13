package nl.rabobank.account;

public record PaymentAccount(String accountNumber, String accountHolderName, Double balance) implements Account {}
