package com.ceramax.api.repository.inventario;

import java.time.Instant;
import java.util.UUID;

public interface MovimientoInventarioProjection {
    UUID getId();
    String getCodigo();
    Instant getCreatedAt();
    UUID getIdAlmacen();
    String getAlmacenNombre();
    UUID getIdProducto();
    String getProductoCodigo();
    String getProductoNombre();
    UUID getIdUsuario();
    String getUsuarioNombre();
    UUID getIdPedido();
    UUID getIdTraslado();
    String getTipo();
    String getMotivo();
    Integer getCantidad();
    Integer getStockResultante();
    String getObservacion();
}
