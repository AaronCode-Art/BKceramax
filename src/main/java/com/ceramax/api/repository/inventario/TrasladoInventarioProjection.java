package com.ceramax.api.repository.inventario;

import java.time.Instant;
import java.util.UUID;

public interface TrasladoInventarioProjection {
    UUID getId();
    String getCodigo();
    String getTipoTraslado();
    String getEstado();
    UUID getIdAlmacenOrigen();
    String getAlmacenOrigenNombre();
    UUID getIdAlmacenDestino();
    String getAlmacenDestinoNombre();
    UUID getIdProducto();
    String getProductoCodigo();
    String getProductoNombre();
    Integer getCantidad();
    UUID getIdPedido();
    String getPedidoCodigo();
    Instant getFechaInicio();
    Integer getDiasTransito();
}
