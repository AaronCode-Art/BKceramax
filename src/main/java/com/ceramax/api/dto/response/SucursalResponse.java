package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SucursalResponse(
    UUID id,
    String codigo,
    String nombre,
    String departamento,
    String provincia,
    String distrito,
    String direccion,
    String referencia,
    String codigoPostal,
    String idUbigeo,
    Boolean activo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
