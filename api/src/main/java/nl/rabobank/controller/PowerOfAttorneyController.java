package nl.rabobank.controller;

import lombok.RequiredArgsConstructor;
import nl.rabobank.dto.PowerOfAttorneyRequest;
import nl.rabobank.dto.PowerOfAttorneyResponse;
import nl.rabobank.service.AccountService;
import nl.rabobank.service.PowerOfAttorneyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/power-of-attorney")
@RequiredArgsConstructor
public class PowerOfAttorneyController {

    private final PowerOfAttorneyService powerOfAttorneyService;

    private final AccountService accountService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PowerOfAttorneyResponse> grantAccess(@RequestBody PowerOfAttorneyRequest request) {
        // TODO: use existsById?
        var accountOpt = accountService.getAccountByNumber(request.getAccountNumber());
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "No account found with number: %s".formatted(request.getAccountNumber()));
        }

        var powerOfAttorney = powerOfAttorneyService.grantAccess(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(powerOfAttorney);
    }
}
