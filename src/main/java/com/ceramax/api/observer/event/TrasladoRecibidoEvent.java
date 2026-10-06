package com.ceramax.api.observer.event;

import java.util.UUID;

public record TrasladoRecibidoEvent(
    UUID trasladoId,
    String codigo,
    UUID almacenDestinoId,
    String almacenDestinoNombre,
    UUID productoId,
    String productoCodigo,
    String productoNombre,
    Integer cantidad,
    UUID usuarioActorId
) {}
