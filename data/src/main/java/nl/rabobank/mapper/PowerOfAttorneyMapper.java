package nl.rabobank.mapper;

import lombok.AllArgsConstructor;
import nl.rabobank.authorizations.PowerOfAttorney;
import nl.rabobank.document.PowerOfAttorneyDocument;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PowerOfAttorneyMapper {

    private AccountMapper accountMapper;

    public PowerOfAttorneyDocument toDocument(PowerOfAttorney domain) {
        return PowerOfAttorneyDocument.builder()
                .granteeName(domain.granteeName())
                .grantorName(domain.grantorName())
                .accountDocument(accountMapper.toDocument(domain.account()))
                .authorization(domain.authorization())
                .build();
    }

    public PowerOfAttorney toDomain(PowerOfAttorneyDocument doc) {
        return PowerOfAttorney.builder()
                .granteeName(doc.getGranteeName())
                .grantorName(doc.getGrantorName())
                .account(accountMapper.toDomain(doc.getAccountDocument()))
                .authorization(doc.getAuthorization())
                .build();
    }
}
