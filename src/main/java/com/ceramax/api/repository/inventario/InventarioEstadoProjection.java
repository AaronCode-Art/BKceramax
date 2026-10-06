package com.ceramax.api.repository.inventario;

import java.util.UUID;

public interface InventarioEstadoProjection {
    UUID getIdAlmacen();
    UUID getIdProducto();
    Boolean getStockBajo();
}
