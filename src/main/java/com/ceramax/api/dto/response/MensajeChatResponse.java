package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MensajeChatResponse(
    UUID id,
    UUID chatId,
    UUID remitenteId,
    String tipoRemitente,
    String contenido,
    Boolean leido,
    OffsetDateTime creadoEn
) {}
