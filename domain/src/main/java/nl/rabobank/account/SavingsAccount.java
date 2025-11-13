package nl.rabobank.account;

public record SavingsAccount(String accountNumber, String accountHolderName, Double balance) implements Account {}
