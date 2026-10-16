package com.ceramax.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void devuelveConflictoCuandoOtraOperacionModificoElRegistro() {
        var response = handler.handleOptimisticLockingFailure(
            new OptimisticLockingFailureException("stale row"),
            new MockHttpServletRequest("PUT", "/api/v1/pedidos/123")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().message().contains("Recargue los datos"));
    }
}
