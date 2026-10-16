package com.ceramax.api.observer.event;

import java.util.UUID;

public record ProductoActualizadoEvent(
    UUID productoId,
    String productoNombre,
    UUID usuarioActorId
) {}
