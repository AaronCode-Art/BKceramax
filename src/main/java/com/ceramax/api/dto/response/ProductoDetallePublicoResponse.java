package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductoDetallePublicoResponse(
    UUID id,
    String codigo,
    String nombre,
    String categoria,
    String descripcion,
    List<Map<String, Object>> especificaciones,
    BigDecimal precio,
    BigDecimal precioFinal,
    BigDecimal descuentoPorcentaje,
    Integer stockDisponible
) {}
