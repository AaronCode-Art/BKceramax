package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SolicitudEntradaResponse(
    UUID id,
    String codigo,
    String tipo,
    String estado,
    UUID idAlmacen,
    String almacenNombre,
    UUID idProducto,
    String productoCodigo,
    String productoNombre,
    Integer cantidad,
    Integer stockResultanteEsperado,
    String motivo,
    String observacion,
    String usuarioSolicitanteNombre,
    String usuarioAprobadorNombre,
    OffsetDateTime fechaSolicitud,
    OffsetDateTime fechaRespuesta,
    String motivoRespuesta
) {}
