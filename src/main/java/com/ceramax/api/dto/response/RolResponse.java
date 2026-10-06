package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RolResponse(
    UUID id,
    String codigo,
    String nombre,
    String descripcion,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
