package nl.rabobank.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleConflict_shouldReturnResponseEntity_forDuplicateAccountException() {
        var exception = new DuplicateAccountException("Account already exists with number: NL123456");

        var response = exceptionHandler.handleConflict(exception);

        assertThat(response).isNotNull().satisfies(resp -> {
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getStatus()).isEqualTo(409);
            assertThat(resp.getBody().getDetail()).isEqualTo("Account already exists with number: NL123456");
        });
    }

    @Test
    void handleAccountNotFound_shouldReturnResponseEntity_forAccountNotFoundException() {
        var exception = new AccountNotFoundException("Account not found with number: NL999999");

        var response = exceptionHandler.handleAccountNotFound(exception);

        assertThat(response).isNotNull().satisfies(resp -> {
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getStatus()).isEqualTo(404);
            assertThat(resp.getBody().getDetail()).isEqualTo("Account not found with number: NL999999");
        });
    }

    @Test
    void handleValidationErrors_shouldReturnResponseEntity_forMethodArgumentNotValidException() {
        var bindingResult = mock(BindingResult.class);
        var fieldError1 = new FieldError("accountRequest", "accountNumber", "accountNumber is required");
        var fieldError2 = new FieldError("accountRequest", "accountHolderName", "accountHolderName is required");

        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        var exception = new MethodArgumentNotValidException(null, bindingResult);

        var response = exceptionHandler.handleValidationErrors(exception);

        assertThat(response).isNotNull().satisfies(resp -> {
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getStatus()).isEqualTo(400);
        });

        String detail = response.getBody().getDetail();
        assertThat(detail)
                .contains("accountNumber")
                .contains("accountNumber is required")
                .contains("accountHolderName")
                .contains("accountHolderName is required");
    }
}
