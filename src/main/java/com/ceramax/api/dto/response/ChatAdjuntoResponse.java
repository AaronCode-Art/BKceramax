package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatAdjuntoResponse(
    UUID id,
    String nombreArchivo,
    String url,
    String tipoContenido,
    Long tamanoBytes,
    OffsetDateTime creadoEn
) {}
