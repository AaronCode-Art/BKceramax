package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PedidoResponse(
    UUID id,
    String codigo,
    String canal,
    String tipoEntrega,
    String estadoCodigo,
    String estadoNombre,
    UUID logisticaId,
    String logisticaNombre,
    UUID deliveryId,
    String deliveryNombre,
    String tipoComprobante,
    String serie,
    String numeroComprobante,
    BigDecimal subtotal,
    BigDecimal descuentoCupon,
    BigDecimal costoEnvio,
    BigDecimal igv,
    BigDecimal total,
    Boolean tieneReserva,
    Boolean despachado,
    OffsetDateTime createdAt,
    List<DetallePedidoResponse> detalles,
    List<PagoResponse> pagos,
    DatosEntregaResponse datosEntrega
) {}
