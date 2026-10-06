package com.ceramax.api.repository.auditoria;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditoriaRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditoriaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID registrar(
        UUID usuarioId,
        String accion,
        String tabla,
        UUID registroId,
        String descripcion,
        String datosAnteriores,
        String datosNuevos
    ) {
        return jdbcTemplate.queryForObject(
            """
                SELECT fn_registrar_auditoria(
                    ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb)
                )
                """,
            UUID.class,
            usuarioId,
            accion,
            tabla,
            registroId,
            descripcion,
            datosAnteriores,
            datosNuevos
        );
    }
}
