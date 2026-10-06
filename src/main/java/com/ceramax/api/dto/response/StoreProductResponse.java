package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record StoreProductResponse(
    UUID id,
    String codigo,
    String nombre,
    String categoria,
    String descripcion,
    BigDecimal precio,
    BigDecimal precioFinal,
    BigDecimal descuentoPorcentaje,
    Integer stockDisponible
) {}
