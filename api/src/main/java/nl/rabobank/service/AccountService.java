package nl.rabobank.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import nl.rabobank.account.Account;
import nl.rabobank.dto.AccountRequest;
import nl.rabobank.dto.AccountResponse;
import nl.rabobank.mapper.AccountApiMapper;
import nl.rabobank.mapper.AccountMapper;
import nl.rabobank.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountResponse createAccount(AccountRequest request) {
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new IllegalArgumentException("Account already exists with number: " + request.getAccountNumber());
        }

        var account = AccountApiMapper.toDomain(request);
        var document = AccountMapper.toDocument(account);
        var saved = AccountMapper.toDomain(accountRepository.save(document));
        return AccountApiMapper.toResponse(saved);
    }

    public Optional<Account> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber).map(AccountMapper::toDomain);
    }
}
