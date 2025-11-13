package nl.rabobank.repository;

import java.util.Optional;
import nl.rabobank.document.AccountDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends MongoRepository<AccountDocument, String> {

    Optional<AccountDocument> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
