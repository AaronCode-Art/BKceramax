package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClienteResponse(
    UUID id,
    String codigo,
    String nombres,
    String apellidos,
    String email,
    String tipoDocumento,
    String numeroDocumento,
    String telefono,
    String departamento,
    String provincia,
    String distrito,
    String direccion,
    String codigoPostal,
    String referencia,
    String idUbigeo,
    Boolean activo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
