package com.ceramax.api.observer.event;

import java.util.UUID;

public record StockBajoEvent(
    UUID almacenId,
    String almacenNombre,
    UUID productoId,
    String productoCodigo,
    String productoNombre,
    Integer stockDisponible,
    Integer stockMinimo,
    UUID usuarioActorId
) {}
