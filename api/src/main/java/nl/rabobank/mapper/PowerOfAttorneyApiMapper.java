package nl.rabobank.mapper;

import lombok.RequiredArgsConstructor;
import nl.rabobank.account.Account;
import nl.rabobank.authorizations.PowerOfAttorney;
import nl.rabobank.dto.PowerOfAttorneyRequest;
import nl.rabobank.dto.PowerOfAttorneyResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PowerOfAttorneyApiMapper {

    private final AccountApiMapper accountApiMapper;

    public PowerOfAttorney toDomain(PowerOfAttorneyRequest request) {
        return toDomain(request, null);
    }

    public PowerOfAttorney toDomain(PowerOfAttorneyRequest request, Account account) {
        return PowerOfAttorney.builder()
                .grantorName(request.getGrantorName())
                .granteeName(request.getGranteeName())
                .authorization(request.getAuthorization())
                .account(account)
                .build();
    }

    public PowerOfAttorneyResponse toResponse(PowerOfAttorney poa) {
        return PowerOfAttorneyResponse.builder()
                .grantorName(poa.grantorName())
                .granteeName(poa.granteeName())
                .authorization(String.valueOf(poa.authorization()))
                .account(accountApiMapper.toResponse(poa.account()))
                .build();
    }
}
