package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatResponse(
    UUID id,
    String codigo,
    String tipo,
    UUID clienteId,
    UUID pedidoId,
    UUID usuarioCreadorId,
    UUID usuarioAsignadoId,
    UUID usuarioDestinoId,
    String asunto,
    String estado,
    OffsetDateTime creadoEn,
    OffsetDateTime actualizadoEn
) {}
