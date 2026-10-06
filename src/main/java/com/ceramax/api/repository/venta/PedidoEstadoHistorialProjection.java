package com.ceramax.api.repository.venta;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface PedidoEstadoHistorialProjection {
    UUID getId();
    String getEstadoAnteriorCodigo();
    String getEstadoCodigo();
    String getAccion();
    UUID getUsuarioId();
    String getUsuarioNombre();
    OffsetDateTime getCreadoEn();
}
