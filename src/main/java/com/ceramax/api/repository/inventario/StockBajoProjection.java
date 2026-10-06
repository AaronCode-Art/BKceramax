package com.ceramax.api.repository.inventario;

import java.util.UUID;

public interface StockBajoProjection {
    UUID getIdAlmacen();
    String getAlmacenNombre();
    UUID getIdProducto();
    String getProductoCodigo();
    String getProductoNombre();
    Integer getStockDisponible();
    Integer getStockMinimo();
}
