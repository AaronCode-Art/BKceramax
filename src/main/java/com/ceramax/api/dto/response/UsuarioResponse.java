package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponse(
    UUID id,
    String codigo,
    String nombres,
    String apellidos,
    String email,
    String tipoDocumento,
    String numeroDocumento,
    UUID rolId,
    String rolCodigo,
    UUID sucursalId,
    String telefono,
    Boolean activo,
    OffsetDateTime createdAt
) {}
