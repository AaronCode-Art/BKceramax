package com.ceramax.api.observer.event;

import java.util.UUID;

public record PedidoCreadoEvent(
    UUID pedidoId,
    String codigoPedido,
    String canal,
    String estadoCodigo,
    String estadoNombre,
    String clienteEmail,
    UUID usuarioActorId
) {}
