package com.ceramax.api.repository.inventario;

import java.util.UUID;

public interface InventarioProjection {
    UUID getIdSucursal();
    String getSucursalNombre();
    UUID getIdAlmacen();
    String getAlmacenCodigo();
    String getAlmacenNombre();
    UUID getIdProducto();
    String getProductoCodigo();
    String getProductoNombre();
    String getCategoriaNombre();
    Integer getStockFisico();
    Integer getStockReservado();
    Integer getStockDisponible();
    Integer getStockMinimo();
    Boolean getStockBajo();
}
