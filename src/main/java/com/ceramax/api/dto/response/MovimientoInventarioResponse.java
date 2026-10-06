package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MovimientoInventarioResponse(
    UUID id,
    String codigo,
    OffsetDateTime fecha,
    UUID almacenId,
    String almacenNombre,
    UUID productoId,
    String productoCodigo,
    String productoNombre,
    UUID usuarioId,
    String usuarioNombre,
    UUID pedidoId,
    UUID trasladoId,
    String tipo,
    String motivo,
    Integer cantidad,
    Integer stockResultante,
    String observacion
) {}
