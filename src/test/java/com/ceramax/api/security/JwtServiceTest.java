package com.ceramax.api.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void invalidaTokensEmitidosAntesDelInicioDelServidor() throws Exception {
        JwtService jwtService = new JwtService();
        setField(jwtService, "secret", "01234567890123456789012345678901");
        setField(jwtService, "expirationMinutes", 60L);
        jwtService.init();
        String token = jwtService.generateToken("cliente@example.test", "CLIENTE");

        assertTrue(jwtService.isTokenValid(token));

        setField(jwtService, "serverStartedAt", Instant.now().plusSeconds(1));

        assertFalse(jwtService.isTokenValid(token));
    }

    private static void setField(JwtService jwtService, String name, Object value) throws Exception {
        Field field = JwtService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(jwtService, value);
    }
}
