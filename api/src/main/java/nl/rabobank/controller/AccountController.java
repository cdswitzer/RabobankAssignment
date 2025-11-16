package nl.rabobank.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.rabobank.apimapper.AccountApiMapper;
import nl.rabobank.dto.AccountRequest;
import nl.rabobank.dto.AccountResponse;
import nl.rabobank.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountApiMapper accountApiMapper;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
        var account = accountService.createAccount(request);
        var response = accountApiMapper.toResponse(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
