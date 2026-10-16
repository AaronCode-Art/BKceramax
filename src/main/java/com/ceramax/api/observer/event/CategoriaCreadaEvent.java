package com.ceramax.api.observer.event;

import java.util.UUID;

public record CategoriaCreadaEvent(
    UUID categoriaId,
    String categoriaNombre,
    UUID usuarioActorId
) {}
