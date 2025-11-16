package nl.rabobank.service;

import lombok.RequiredArgsConstructor;
import nl.rabobank.apimapper.PowerOfAttorneyApiMapper;
import nl.rabobank.authorizations.PowerOfAttorney;
import nl.rabobank.dto.PowerOfAttorneyRequest;
import nl.rabobank.exception.UnknownAccountException;
import nl.rabobank.mapper.AccountMapper;
import nl.rabobank.mapper.PowerOfAttorneyMapper;
import nl.rabobank.repository.AccountRepository;
import nl.rabobank.repository.PowerOfAttorneyRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PowerOfAttorneyService {

    private final PowerOfAttorneyRepository powerOfAttorneyRepository;
    private final PowerOfAttorneyApiMapper powerOfAttorneyApiMapper;
    private final PowerOfAttorneyMapper powerOfAttorneyMapper;

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public PowerOfAttorney grantAccess(PowerOfAttorneyRequest request) {
        var accountDocument = accountRepository
                .findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new UnknownAccountException(
                        "No account found for accountnumber: %s.".formatted(request.getAccountNumber())));

        var account = accountMapper.toDomain(accountDocument);
        var powerOfAttorney = powerOfAttorneyApiMapper.toDomain(request, account);
        var document = powerOfAttorneyMapper.toDocument(powerOfAttorney);

        return powerOfAttorneyMapper.toDomain(powerOfAttorneyRepository.save(document));
    }

    //    public List<PowerOfAttorney> getPowerOfAttorneysForGrantee(String grantee) {
    //        return repository.findByGrantee(grantee)
    //                .stream()
    //                .map(PowerOfAttorneyMapper::toDomain)
    //                .toList();
    //    }
}
