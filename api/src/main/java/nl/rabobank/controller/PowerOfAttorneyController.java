package nl.rabobank.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import nl.rabobank.apimapper.PowerOfAttorneyApiMapper;
import nl.rabobank.dto.PowerOfAttorneyRequest;
import nl.rabobank.dto.PowerOfAttorneyResponse;
import nl.rabobank.exception.AccountNotFoundException;
import nl.rabobank.service.AccountService;
import nl.rabobank.service.PowerOfAttorneyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/power-of-attorney")
@RequiredArgsConstructor
public class PowerOfAttorneyController {

    private final PowerOfAttorneyService powerOfAttorneyService;
    private final AccountService accountService;
    private final PowerOfAttorneyApiMapper powerOfAttorneyApiMapper;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PowerOfAttorneyResponse> grantAccess(@Valid @RequestBody PowerOfAttorneyRequest request) {
        if (!accountService.existsByAccountNumber(request.getAccountNumber())) {
            throw new AccountNotFoundException(
                    "No account found with number: %s".formatted(request.getAccountNumber()));
        }

        var powerOfAttorney = powerOfAttorneyService.grantAccess(request);
        var response = powerOfAttorneyApiMapper.toResponse(powerOfAttorney);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PowerOfAttorneyResponse>> listByGrantee(
            @RequestParam("granteeName") String granteeName) {

        var list = powerOfAttorneyService.findByGranteeName(granteeName);
        var response = list.stream().map(powerOfAttorneyApiMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }
}
