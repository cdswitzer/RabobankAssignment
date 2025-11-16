package nl.rabobank.repository;

import nl.rabobank.document.PowerOfAttorneyDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PowerOfAttorneyRepository extends MongoRepository<PowerOfAttorneyDocument, String> {}
