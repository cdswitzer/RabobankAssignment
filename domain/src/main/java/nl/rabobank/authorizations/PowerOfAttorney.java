package nl.rabobank.authorizations;

import lombok.Builder;
import nl.rabobank.account.Account;

@Builder(toBuilder = true)
public record PowerOfAttorney(String granteeName, String grantorName, Account account, Authorization authorization) {}
