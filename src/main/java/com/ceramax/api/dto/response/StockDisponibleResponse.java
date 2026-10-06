package com.ceramax.api.dto.response;

import java.util.UUID;

public record StockDisponibleResponse(
    UUID sucursalId,
    String sucursalNombre,
    UUID almacenId,
    String almacenCodigo,
    String almacenNombre,
    UUID productoId,
    String productoCodigo,
    String productoNombre,
    String categoriaNombre,
    Integer stockFisico,
    Integer stockReservado,
    Integer stockDisponible
) {}
