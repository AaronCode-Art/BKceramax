package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CategoriaResponse(
    UUID id,
    String codigo,
    String nombre,
    String descripcion,
    Boolean activo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
