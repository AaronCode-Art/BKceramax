package com.ceramax.api.repository.inventario;

import java.time.Instant;
import java.util.UUID;

public interface ReservaInventarioProjection {
    UUID getIdReserva();
    UUID getIdPedido();
    String getPedidoCodigo();
    UUID getIdDetallePedido();
    UUID getIdAlmacen();
    String getAlmacenNombre();
    UUID getIdProducto();
    Integer getCantidad();
    String getEstado();
    String getTipoEntrega();
    String getClienteNombre();
    Instant getFechaPedido();
    Instant getFechaReserva();
}
