package nl.rabobank.mapper;

import nl.rabobank.authorizations.PowerOfAttorney;
import nl.rabobank.document.PowerOfAttorneyDocument;
import org.springframework.stereotype.Component;

@Component
public class PowerOfAttorneyMapper {

    public PowerOfAttorneyDocument toDocument(PowerOfAttorney domain) {
        return PowerOfAttorneyDocument.builder()
                .granteeName(domain.granteeName())
                .grantorName(domain.grantorName())
                .account(domain.account())
                .authorization(domain.authorization())
                .build();
    }

    public PowerOfAttorney toDomain(PowerOfAttorneyDocument doc) {
        return PowerOfAttorney.builder()
                .granteeName(doc.getGranteeName())
                .grantorName(doc.getGrantorName())
                .account(doc.getAccount())
                .authorization(doc.getAuthorization())
                .build();
    }
}
