package com.ceramax.api.repository.inventario;

import java.time.Instant;
import java.util.UUID;

public interface AjusteInventarioProjection {
    UUID getId();
    String getCodigo();
    UUID getIdUsuarioEjecutor();
    String getUsuarioEjecutor();
    UUID getIdAlmacen();
    String getAlmacenNombre();
    UUID getIdProducto();
    String getProductoCodigo();
    String getProductoNombre();
    Integer getStockAnterior();
    Integer getCantidadAjustada();
    Integer getStockNuevo();
    String getMotivo();
    String getObservacion();
    Instant getCreatedAt();
}
