package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TrasladoInventarioResponse(
    UUID id,
    String codigo,
    String tipo,
    String estado,
    UUID almacenOrigenId,
    String almacenOrigenNombre,
    UUID almacenDestinoId,
    String almacenDestinoNombre,
    UUID productoId,
    String productoCodigo,
    String productoNombre,
    Integer cantidad,
    UUID pedidoId,
    String pedidoCodigo,
    OffsetDateTime fechaInicio,
    Integer diasTransito
) {}
