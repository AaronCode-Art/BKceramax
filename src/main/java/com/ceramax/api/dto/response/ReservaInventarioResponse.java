package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservaInventarioResponse(
    UUID id,
    UUID pedidoId,
    String pedidoCodigo,
    UUID detallePedidoId,
    UUID almacenId,
    String almacenNombre,
    UUID productoId,
    Integer cantidad,
    String estado,
    String tipoEntrega,
    String clienteNombre,
    OffsetDateTime fechaPedido,
    OffsetDateTime fechaReserva
) {}
