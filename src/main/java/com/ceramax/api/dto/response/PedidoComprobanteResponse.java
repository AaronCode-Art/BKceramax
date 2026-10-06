package com.ceramax.api.dto.response;

import java.time.OffsetDateTime;

public record PedidoComprobanteResponse(
    PedidoResponse pedido,
    String clienteNombre,
    String clienteTipoDocumento,
    String clienteNumeroDocumento,
    String clienteEmail,
    String ruc,
    String razonSocial,
    String sucursalNombre,
    String sucursalDireccion,
    OffsetDateTime fechaEmision
) {}
