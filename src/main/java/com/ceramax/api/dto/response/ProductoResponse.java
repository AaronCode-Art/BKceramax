package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductoResponse(
    UUID id,
    String codigo,
    UUID categoriaId,
    String categoriaNombre,
    String nombre,
    String descripcion,
    List<Map<String, Object>> especificaciones,
    BigDecimal precio,
    BigDecimal descuentoPorcentaje,
    BigDecimal precioFinal,
    Boolean activo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
