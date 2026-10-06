package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GaleriaImagenResponse(
    UUID id,
    UUID productoId,
    String url,
    String publicId,
    Short orden,
    Boolean esPrincipal,
    OffsetDateTime createdAt
) {}
