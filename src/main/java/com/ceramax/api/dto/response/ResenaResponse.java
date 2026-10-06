package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ResenaResponse(
    UUID id,
    UUID productoId,
    String productoCodigo,
    String productoNombre,
    UUID clienteId,
    String clienteNombre,
    UUID pedidoId,
    Short calificacion,
    String comentario,
    OffsetDateTime creadoEn,
    List<ResenaImagenResponse> imagenes
) {}
