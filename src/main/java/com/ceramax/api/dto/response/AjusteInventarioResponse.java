package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AjusteInventarioResponse(
    UUID id,
    String codigo,
    UUID usuarioId,
    String usuarioNombre,
    UUID almacenId,
    String almacenNombre,
    UUID productoId,
    String productoCodigo,
    String productoNombre,
    Integer stockAnterior,
    Integer cantidadAjustada,
    Integer stockNuevo,
    String motivo,
    String observacion,
    OffsetDateTime fecha
) {}
