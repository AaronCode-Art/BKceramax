package com.ceramax.api.dto.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class ClienteRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsCompleteRegistrationWithAccentedNames() {
        assertTrue(validator.validate(request("DNI", "12345678", "María-José", "O'Connor", "999123456")).isEmpty());
    }

    @Test
    void rejectsInvalidDocumentAndNonNumericPhone() {
        assertFalse(validator.validate(request("DNI", "1234", "María", "Pérez", "999-123")).isEmpty());
    }

    private ClienteRequest request(String documentType, String documentNumber, String names, String surnames, String phone) {
        return new ClienteRequest(
            documentType,
            documentNumber,
            names,
            surnames,
            "cliente@example.test",
            "ClaveSegura123",
            phone,
            "Lima",
            "Lima",
            "Lima",
            "Jr. Ejemplo 123",
            "15001",
            "Frente al parque",
            "150101"
        );
    }
}
