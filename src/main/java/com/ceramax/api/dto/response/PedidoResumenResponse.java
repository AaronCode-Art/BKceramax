package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PedidoResumenResponse(
    UUID id,
    String codigo,
    String canal,
    String tipoEntrega,
    String estadoCodigo,
    String estadoNombre,
    String tipoComprobante,
    String serie,
    String numeroComprobante,
    BigDecimal total,
    OffsetDateTime createdAt,
    DatosEntregaResponse datosEntrega
) {}
