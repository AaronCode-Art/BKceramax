package com.ceramax.api.dto.response;

import java.util.UUID;

public record VentaPresencialResponse(
    UUID pedidoId,
    String codigoPedido,
    String tipoResolucion,
    String mensaje
) {}
