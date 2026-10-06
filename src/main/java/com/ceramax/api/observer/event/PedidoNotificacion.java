package com.ceramax.api.observer.event;

import java.util.UUID;

public record PedidoNotificacion(
    UUID pedidoId,
    String codigoPedido,
    String canal,
    String estadoCodigo,
    String estadoNombre
) {}
