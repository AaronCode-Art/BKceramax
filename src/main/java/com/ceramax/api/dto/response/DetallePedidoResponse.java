package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record DetallePedidoResponse(
    UUID id,
    UUID productoId,
    String productoCodigo,
    String productoNombre,
    String categoriaNombre,
    String imagenUrl,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal descuentoPorcentaje,
    BigDecimal subtotal,
    String estadoStock
) {}
