package nl.rabobank.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.rabobank.authorizations.Authorization;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "power_of_attorney_grants")
public class PowerOfAttorneyDocument {
    @Id
    String id;

    String granteeName;
    String grantorName;
    AccountDocument accountDocument;
    Authorization authorization;
}
