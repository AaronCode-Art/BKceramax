package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PedidoEstadoHistorialResponse(
    UUID id,
    String estadoAnteriorCodigo,
    String estadoCodigo,
    String accion,
    UUID usuarioId,
    String usuarioNombre,
    OffsetDateTime creadoEn
) {}
