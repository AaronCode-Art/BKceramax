package com.ceramax.api.observer.event;

import java.util.UUID;

public record PedidoEstadoCambiadoEvent(
    UUID pedidoId,
    String codigoPedido,
    String estadoCodigo,
    String estadoNombre,
    String clienteEmail,
    String deliveryEmail,
    String estadoAnteriorCodigo,
    UUID usuarioActorId,
    String accion
) {}
