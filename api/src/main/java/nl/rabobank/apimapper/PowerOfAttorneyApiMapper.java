package nl.rabobank.apimapper;

import lombok.RequiredArgsConstructor;
import nl.rabobank.account.Account;
import nl.rabobank.authorizations.Authorization;
import nl.rabobank.authorizations.PowerOfAttorney;
import nl.rabobank.dto.PowerOfAttorneyRequest;
import nl.rabobank.dto.PowerOfAttorneyResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PowerOfAttorneyApiMapper {

    private final AccountApiMapper accountApiMapper;

    public PowerOfAttorney toDomain(PowerOfAttorneyRequest request, Account account) {
        return PowerOfAttorney.builder()
                .grantorName(request.getGrantorName())
                .granteeName(request.getGranteeName())
                .authorization(Authorization.valueOf(request.getAuthorization()))
                .account(account)
                .build();
    }

    public PowerOfAttorneyResponse toResponse(PowerOfAttorney powerOfAttorney) {
        return PowerOfAttorneyResponse.builder()
                .grantorName(powerOfAttorney.grantorName())
                .granteeName(powerOfAttorney.granteeName())
                .authorization(String.valueOf(powerOfAttorney.authorization()))
                .account(accountApiMapper.toResponse(powerOfAttorney.account()))
                .build();
    }
}
