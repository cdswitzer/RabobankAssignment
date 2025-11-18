package nl.rabobank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import nl.rabobank.controller.AccountController;
import nl.rabobank.controller.PowerOfAttorneyController;
import nl.rabobank.dto.AccountRequest;
import nl.rabobank.dto.AccountResponse;
import nl.rabobank.dto.PowerOfAttorneyRequest;
import nl.rabobank.dto.PowerOfAttorneyResponse;
import nl.rabobank.exception.AccountNotFoundException;
import nl.rabobank.exception.DuplicateAccountException;
import nl.rabobank.repository.AccountRepository;
import nl.rabobank.repository.PowerOfAttorneyRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
class IntegrationTest {

    @Autowired
    private AccountController accountController;

    @Autowired
    private PowerOfAttorneyController powerOfAttorneyController;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PowerOfAttorneyRepository powerOfAttorneyRepository;

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0.14"))
            .withExposedPorts(27017)
            .withReuse(true);

    @BeforeAll
    static void logMongoInfo() {
        mongoDBContainer.setPortBindings(List.of("32819:27017"));
        mongoDBContainer.start();
    }

    @BeforeEach
    void setUp() {
        powerOfAttorneyRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @ParameterizedTest
    @CsvSource({
        "NL123456, John Doe, 1000.0, PAYMENT",
        "NL654321, Mary Doe, 500.0, SAVINGS",
        "NL456789, John Doe, -1000.0, SAVINGS",
        "NL987654, Mary Doe, -500.0, SAVINGS"
    })
    void postAccounts_shouldCreateAccountAndVerifyInDatabase_forAccountRequest(
            String accountNumber, String accountHolderName, Double initialBalance, String accountType) {
        var request = getAccountRequest(accountNumber, accountHolderName, initialBalance, accountType);
        var responseEntityForCreation = accountController.create(request);

        assertAccountCreationResponseEntity(
                accountNumber, accountHolderName, initialBalance, accountType, responseEntityForCreation);

        var responseEntityForFind = accountController.listAll();

        assertThat(responseEntityForFind).isNotNull().satisfies(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
            assertThat(response.hasBody()).isTrue();
            assertThat(response.getBody()).isNotNull().satisfies(body -> {
                assertThat(body).hasSize(1);
                assertThat(body.getFirst().getAccountNumber()).isEqualTo(accountNumber);
                assertThat(body.getFirst().getAccountHolderName()).isEqualTo(accountHolderName);
                assertThat(body.getFirst().getBalance()).isEqualTo(initialBalance.toString());
                assertThat(body.getFirst().getAccountType()).isEqualTo(accountType);
            });
        });
    }

    @ParameterizedTest
    @CsvSource({
        "John Doe, Frank Bank, READ, NL123456, 1000.0, PAYMENT, Grant READ",
        "Mary Doe, Pieter Post, WRITE, NL654321, 500.0, SAVINGS, Grant WRITE",
    })
    void postPowerOfAttorney_shouldCreateAccountAndGrantReadPowerOfAttorney_forPowerOfAttorneyRequest(
            String grantorName,
            String granteeName,
            String authorization,
            String accountNumber,
            Double balance,
            String accountType) {

        var accountRequest = getAccountRequest(accountNumber, grantorName, balance, accountType);
        var accountResponseResponseEntity = accountController.create(accountRequest);
        assertAccountCreationResponseEntity(
                accountNumber, grantorName, balance, accountType, accountResponseResponseEntity);

        var powerOfAttorneyRequest =
                getPowerOfAttorneyRequest(grantorName, granteeName, authorization, accountNumber, accountType);
        var powerOfAttorneyResponseEntity = powerOfAttorneyController.grantAccess(powerOfAttorneyRequest);
        assertPowerOfAttorneyCreationResponseEntity(
                grantorName, granteeName, authorization, accountNumber, powerOfAttorneyResponseEntity);

        var allPowerOfAttorneyDocumentsFromDatabase = powerOfAttorneyController.list(null);
        assertStatusAndListSizeFromDatabase(allPowerOfAttorneyDocumentsFromDatabase, 1);

        var databaseResponseEntity = powerOfAttorneyController.list(granteeName);

        assertThat(databaseResponseEntity.getBody()).isNotNull().satisfies(body -> {
            assertThat(body).isNotNull();
            assertThat(body).hasSize(1);
            assertThat(body.getFirst().getGranteeName()).isEqualTo(granteeName);
            assertThat(body.getFirst().getGrantorName()).isEqualTo(grantorName);
            assertThat(body.getFirst().getAuthorization()).isEqualTo(authorization);
            assertThat(body.getFirst().getAccount()).isNotNull();
            assertThat(body.getFirst().getAccount().getAccountNumber()).isEqualTo(accountNumber);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "John Doe, Frank Bank, Peter Johnson, READ, WRITE, NL123456, 1000.0, PAYMENT",
        "Mary Doe, Pieter Post, Alice Dark, WRITE, READ, NL654321, 500.0, SAVINGS",
    })
    void postPowerOfAttorney_shouldHandleMultipleGrantsForSameAccount_forPowerOfAttorneyRequest(
            String grantorName,
            String firstGranteeName,
            String secondGranteeName,
            String firstAuthorization,
            String secondAuthorization,
            String accountNumber,
            Double balance,
            String accountType) {

        var accountRequest = getAccountRequest(accountNumber, grantorName, balance, accountType);
        var accountResponseResponseEntity = accountController.create(accountRequest);
        assertAccountCreationResponseEntity(
                accountNumber, grantorName, balance, accountType, accountResponseResponseEntity);

        var firstPowerOfAttorneyRequest = getPowerOfAttorneyRequest(
                grantorName, firstGranteeName, firstAuthorization, accountNumber, accountType);
        var firstPowerOfAttorneyResponseEntity = powerOfAttorneyController.grantAccess(firstPowerOfAttorneyRequest);

        assertPowerOfAttorneyCreationResponseEntity(
                grantorName, firstGranteeName, firstAuthorization, accountNumber, firstPowerOfAttorneyResponseEntity);

        var secondPowerOfAttorneyRequest = getPowerOfAttorneyRequest(
                grantorName, secondGranteeName, secondAuthorization, accountNumber, accountType);
        var secondPowerOfAttorneyResponseEntity = powerOfAttorneyController.grantAccess(secondPowerOfAttorneyRequest);

        assertPowerOfAttorneyCreationResponseEntity(
                grantorName,
                secondGranteeName,
                secondAuthorization,
                accountNumber,
                secondPowerOfAttorneyResponseEntity);

        var allPowerOfAttorneyDocumentsFromDatabase = powerOfAttorneyController.list(null);
        assertStatusAndListSizeFromDatabase(allPowerOfAttorneyDocumentsFromDatabase, 2);

        assertThat(allPowerOfAttorneyDocumentsFromDatabase.getBody())
                .extracting(PowerOfAttorneyResponse::getGranteeName)
                .containsExactlyInAnyOrderElementsOf(List.of(firstGranteeName, secondGranteeName));

        assertThat(allPowerOfAttorneyDocumentsFromDatabase.getBody())
                .extracting(PowerOfAttorneyResponse::getAuthorization)
                .containsExactlyInAnyOrderElementsOf(List.of(firstAuthorization, secondAuthorization));
    }

    @Test
    void get_shouldReturnAllAccounts_fromDatabase() {
        var accountsRequests = List.of(
                getAccountRequest("NL111111111", "User One", 1000.0, "PAYMENT"),
                getAccountRequest("NL222222222", "User Two", 2000.0, "SAVINGS"),
                getAccountRequest("NL333333333", "User Three", 3000.0, "PAYMENT"));

        var accountResponseResponseEntities =
                accountsRequests.stream().map(accountController::create).toList();
        accountResponseResponseEntities.forEach(accountResponse -> {
            accountsRequests.stream()
                    .filter(account -> {
                        Assertions.assertNotNull(accountResponse.getBody());
                        return account.getAccountNumber()
                                .equals(accountResponse.getBody().getAccountNumber());
                    })
                    .findFirst()
                    .ifPresent(account -> assertAccountCreationResponseEntity(
                            account.getAccountNumber(),
                            account.getAccountHolderName(),
                            account.getInitialBalance(),
                            account.getAccountType(),
                            accountResponse));
        });

        var allAccountDocumentsFromDatabase = accountController.listAll();
        assertStatusAndListSizeFromDatabase(allAccountDocumentsFromDatabase, 3);

        assertThat(allAccountDocumentsFromDatabase.getBody())
                .extracting(AccountResponse::getAccountNumber)
                .containsExactlyInAnyOrder("NL111111111", "NL222222222", "NL333333333");

        assertThat(allAccountDocumentsFromDatabase.getBody())
                .extracting(AccountResponse::getBalance)
                .containsExactlyInAnyOrder("1000.0", "2000.0", "3000.0");
    }

    @Test
    void get_shouldReturnAllPowerOfAttorney_fromDatabase() {
        var accountRequest = getAccountRequest("NL555555555", "Main User", 5000.0, "SAVINGS");
        var accountResponseResponseEntity = accountController.create(accountRequest);
        assertAccountCreationResponseEntity(
                "NL555555555", "Main User", 5000.0, "SAVINGS", accountResponseResponseEntity);

        var powerOfAttorneyRequests = List.of(
                getPowerOfAttorneyRequest("Main User", "Grantee One", "READ", "NL555555555", "SAVINGS"),
                getPowerOfAttorneyRequest("Main User", "Grantee Two", "WRITE", "NL555555555", "SAVINGS"),
                getPowerOfAttorneyRequest("Main User", "Grantee Three", "READ", "NL555555555", "SAVINGS"));

        var powerOfAttorneyResponseResponseEntities = powerOfAttorneyRequests.stream()
                .map(powerOfAttorneyController::grantAccess)
                .toList();
        powerOfAttorneyResponseResponseEntities.forEach(powerOfAttorneyResponse -> {
            powerOfAttorneyRequests.stream()
                    .filter(powerOfAttorneyRequest -> {
                        Assertions.assertNotNull(powerOfAttorneyResponse.getBody());
                        return powerOfAttorneyRequest
                                .getGranteeName()
                                .equals(powerOfAttorneyResponse.getBody().getGranteeName());
                    })
                    .findFirst()
                    .ifPresent(powerOfAttorneyRequest -> assertPowerOfAttorneyCreationResponseEntity(
                            powerOfAttorneyRequest.getGrantorName(),
                            powerOfAttorneyRequest.getGranteeName(),
                            powerOfAttorneyRequest.getAuthorization(),
                            powerOfAttorneyRequest.getAccountNumber(),
                            powerOfAttorneyResponse));
        });

        var allPowerOfAttorneyDocumentsFromDatabase = powerOfAttorneyController.list(null);
        assertStatusAndListSizeFromDatabase(allPowerOfAttorneyDocumentsFromDatabase, 3);

        assertThat(allPowerOfAttorneyDocumentsFromDatabase.getBody())
                .extracting(PowerOfAttorneyResponse::getGranteeName)
                .containsExactlyInAnyOrder("Grantee One", "Grantee Two", "Grantee Three");

        assertThat(allPowerOfAttorneyDocumentsFromDatabase.getBody())
                .filteredOn(powerOfAttorneyResponse ->
                        powerOfAttorneyResponse.getAuthorization().equals("READ"))
                .hasSize(2);

        assertThat(allPowerOfAttorneyDocumentsFromDatabase.getBody())
                .filteredOn(powerOfAttorneyResponse ->
                        powerOfAttorneyResponse.getAuthorization().equals("WRITE"))
                .hasSize(1);
    }

    @Test
    void post_shouldRejectCreation_forDuplicateAccount() throws Exception {
        var accountRequest = getAccountRequest("NL999999999", "Duplicate Test", 1000.0, "PAYMENT");
        var accountResponseResponseEntity = accountController.create(accountRequest);
        assertAccountCreationResponseEntity(
                "NL999999999", "Duplicate Test", 1000.0, "PAYMENT", accountResponseResponseEntity);

        var duplicateAccountRequest = getAccountRequest("NL999999999", "Other name", 1000.0, "PAYMENT");
        assertThatThrownBy(() -> accountController.create(duplicateAccountRequest))
                .isInstanceOf(DuplicateAccountException.class)
                .hasMessage("Account already exists with number: NL999999999");

        var allAccountDocumentsFromDatabase = accountController.listAll();

        assertThat(allAccountDocumentsFromDatabase).isNotNull().satisfies(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
            assertThat(response.hasBody()).isTrue();
            assertThat(response.getBody()).isNotNull().satisfies(body -> {
                assertThat(body).hasSize(1);
                assertThat(body.getFirst().getAccountNumber()).isEqualTo("NL999999999");
                assertThat(body.getFirst().getAccountHolderName()).isEqualTo("Duplicate Test");
                assertThat(body.getFirst().getBalance()).isEqualTo("1000.0");
                assertThat(body.getFirst().getAccountType()).isEqualTo("PAYMENT");
            });
        });
    }

    @Test
    void post_shouldRejectPowerOfAttorney_forNonExistentAccount() throws Exception {
        var powerOfAttorneyRequest =
                getPowerOfAttorneyRequest("Invalid Grantor", "Invalid Grantee", "READ", "NL000000000", "PAYMENT");
        assertThatThrownBy(() -> powerOfAttorneyController.grantAccess(powerOfAttorneyRequest))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("No account found with number: NL000000000");

        var allPowerOfAttorneyDocumentsFromDatabase = powerOfAttorneyController.list(null);
        assertStatusAndListSizeFromDatabase(allPowerOfAttorneyDocumentsFromDatabase, 0);
    }

    @Test
    void post_shouldCreateAndVerifyInDatabase_foMultipleAccountsAndGrants() {
        var accountsRequests = List.of(
                getAccountRequest("NL100100100", "Alice", 1000.0, "PAYMENT"),
                getAccountRequest("NL200200200", "Bob", 2000.0, "SAVINGS"),
                getAccountRequest("NL300300300", "Charlie", 3000.0, "PAYMENT"));

        var accountResponseResponseEntities =
                accountsRequests.stream().map(accountController::create).toList();
        accountResponseResponseEntities.forEach(accountResponse -> {
            accountsRequests.stream()
                    .filter(account -> {
                        Assertions.assertNotNull(accountResponse.getBody());
                        return account.getAccountNumber()
                                .equals(accountResponse.getBody().getAccountNumber());
                    })
                    .findFirst()
                    .ifPresent(account -> assertAccountCreationResponseEntity(
                            account.getAccountNumber(),
                            account.getAccountHolderName(),
                            account.getInitialBalance(),
                            account.getAccountType(),
                            accountResponse));
        });

        var powerOfAttorneyRequests = List.of(
                getPowerOfAttorneyRequest("Alice", "Bob", "READ", "NL100100100", "PAYMENT"),
                getPowerOfAttorneyRequest("Alice", "Charlie", "WRITE", "NL100100100", "PAYMENT"),
                getPowerOfAttorneyRequest("Bob", "Alice", "READ", "NL200200200", "SAVINGS"),
                getPowerOfAttorneyRequest("Charlie", "Alice", "WRITE", "NL300300300", "PAYMENT"));

        var powerOfAttorneyResponseResponseEntities = powerOfAttorneyRequests.stream()
                .map(powerOfAttorneyController::grantAccess)
                .toList();
        powerOfAttorneyResponseResponseEntities.forEach(powerOfAttorneyResponse -> {
            powerOfAttorneyRequests.stream()
                    .filter(powerOfAttorneyRequest -> {
                        Assertions.assertNotNull(powerOfAttorneyResponse.getBody());
                        return powerOfAttorneyRequest
                                        .getGranteeName()
                                        .equals(powerOfAttorneyResponse
                                                .getBody()
                                                .getGranteeName())
                                && powerOfAttorneyRequest
                                        .getAccountNumber()
                                        .equals(powerOfAttorneyResponse
                                                .getBody()
                                                .getAccount()
                                                .getAccountNumber());
                    })
                    .findFirst()
                    .ifPresent(powerOfAttorneyRequest -> assertPowerOfAttorneyCreationResponseEntity(
                            powerOfAttorneyRequest.getGrantorName(),
                            powerOfAttorneyRequest.getGranteeName(),
                            powerOfAttorneyRequest.getAuthorization(),
                            powerOfAttorneyRequest.getAccountNumber(),
                            powerOfAttorneyResponse));
        });

        var allAccountDocumentsFromDatabase = accountController.listAll();
        assertStatusAndListSizeFromDatabase(allAccountDocumentsFromDatabase, 3);

        var allPowerOfAttorneyDocumentsFromDatabase = powerOfAttorneyController.list(null);
        assertStatusAndListSizeFromDatabase(allPowerOfAttorneyDocumentsFromDatabase, 4);

        assertThat(allPowerOfAttorneyDocumentsFromDatabase.getBody())
                .filteredOn(powerOfAttorneyResponse ->
                        powerOfAttorneyResponse.getGrantorName().equals("Alice"))
                .hasSize(2)
                .extracting(
                        PowerOfAttorneyResponse::getGrantorName,
                        PowerOfAttorneyResponse::getGranteeName,
                        powerOfAttorneyResponse ->
                                powerOfAttorneyResponse.getAccount().getAccountNumber())
                .containsExactlyInAnyOrder(
                        tuple("Alice", "Bob", "NL100100100"), tuple("Alice", "Charlie", "NL100100100"));

        assertThat(allPowerOfAttorneyDocumentsFromDatabase.getBody())
                .filteredOn(powerOfAttorneyResponse ->
                        powerOfAttorneyResponse.getAccount().getAccountNumber().equals("NL100100100"))
                .hasSize(2)
                .extracting(PowerOfAttorneyResponse::getGranteeName)
                .containsExactlyInAnyOrder("Bob", "Charlie");

        assertThat(allPowerOfAttorneyDocumentsFromDatabase.getBody())
                .filteredOn(powerOfAttorneyResponse ->
                        powerOfAttorneyResponse.getGranteeName().equals("Alice"))
                .hasSize(2)
                .extracting(
                        PowerOfAttorneyResponse::getGrantorName,
                        PowerOfAttorneyResponse::getGranteeName,
                        powerOfAttorneyResponse ->
                                powerOfAttorneyResponse.getAccount().getAccountNumber())
                .containsExactlyInAnyOrder(
                        tuple("Bob", "Alice", "NL200200200"), tuple("Charlie", "Alice", "NL300300300"));
    }

    private static AccountRequest getAccountRequest(
            String accountNumber, String accountHolderName, Double initialBalance, String accountType) {
        return AccountRequest.builder()
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .initialBalance(initialBalance)
                .accountType(accountType)
                .build();
    }

    private static PowerOfAttorneyRequest getPowerOfAttorneyRequest(
            String grantorName, String granteeName, String authorization, String accountNumber, String accountType) {
        return PowerOfAttorneyRequest.builder()
                .grantorName(grantorName)
                .granteeName(granteeName)
                .authorization(authorization)
                .accountNumber(accountNumber)
                .accountType(accountType)
                .build();
    }

    private static void assertAccountCreationResponseEntity(
            String accountNumber,
            String accountHolderName,
            Double initialBalance,
            String accountType,
            ResponseEntity<AccountResponse> responseEntityForCreation) {
        assertThat(responseEntityForCreation).isNotNull().satisfies(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
            assertThat(response.hasBody()).isTrue();
            assertThat(response.getBody()).satisfies(body -> {
                assertThat(body.getAccountNumber()).isEqualTo(accountNumber);
                assertThat(body.getAccountHolderName()).isEqualTo(accountHolderName);
                assertThat(body.getBalance()).isEqualTo(initialBalance.toString());
                assertThat(body.getAccountType()).isEqualTo(accountType);
            });
        });
    }

    private static void assertPowerOfAttorneyCreationResponseEntity(
            String grantorName,
            String granteeName,
            String authorization,
            String accountNumber,
            ResponseEntity<PowerOfAttorneyResponse> powerOfAttorneyResponseResponseEntity) {
        assertThat(powerOfAttorneyResponseResponseEntity).isNotNull().satisfies(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(201));
            assertThat(response.hasBody()).isTrue();
            assertThat(response.getBody()).isNotNull().satisfies(body -> {
                assertThat(body).isNotNull();
                assertThat(body.getGranteeName()).isEqualTo(granteeName);
                assertThat(body.getGrantorName()).isEqualTo(grantorName);
                assertThat(body.getAuthorization()).isEqualTo(authorization);
                assertThat(body.getAccount()).isNotNull();
                assertThat(body.getAccount().getAccountNumber()).isEqualTo(accountNumber);
            });
        });
    }

    private static void assertStatusAndListSizeFromDatabase(ResponseEntity<? extends List<?>> documents, int size) {
        assertThat(documents).isNotNull().satisfies(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
            assertThat(response.hasBody()).isTrue();
            assertThat(response.getBody()).isNotNull().satisfies(body -> {
                assertThat(body).isNotNull();
                assertThat(body).hasSize(size);
            });
        });
    }
}
