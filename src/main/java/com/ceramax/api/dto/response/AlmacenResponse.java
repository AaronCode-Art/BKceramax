package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AlmacenResponse(
    UUID id,
    String codigo,
    String nombre,
    UUID sucursalId,
    String sucursalNombre,
    Boolean activo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
