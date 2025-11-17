package nl.rabobank.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import nl.rabobank.account.Account;
import nl.rabobank.apimapper.AccountApiMapper;
import nl.rabobank.dto.AccountRequest;
import nl.rabobank.exception.AccountNotFoundException;
import nl.rabobank.exception.DuplicateAccountException;
import nl.rabobank.mapper.AccountMapper;
import nl.rabobank.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountApiMapper accountApiMapper;
    private final AccountMapper accountMapper;

    public Account createAccount(AccountRequest request) {
        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new DuplicateAccountException("Account already exists with number: " + request.getAccountNumber());
        }

        var account = accountApiMapper.toDomain(request);
        var document = accountMapper.toDocument(account);

        return accountMapper.toDomain(accountRepository.save(document));
    }

    public Account getByAccountNumber(String accountNumber) {
        return accountRepository
                .findByAccountNumber(accountNumber)
                .map(accountMapper::toDomain)
                .orElseThrow(() ->
                        new AccountNotFoundException("Account with number '%s' not found".formatted(accountNumber)));
    }

    public List<Account> findAll() {
        return accountRepository.findAll().stream().map(accountMapper::toDomain).toList();
    }

    public boolean existsByAccountNumber(String accountNumber) {
        return accountRepository.existsByAccountNumber(accountNumber);
    }
}
