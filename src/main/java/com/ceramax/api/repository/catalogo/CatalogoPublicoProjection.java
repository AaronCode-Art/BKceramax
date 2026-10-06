package com.ceramax.api.repository.catalogo;

import java.math.BigDecimal;
import java.util.UUID;

public interface CatalogoPublicoProjection {
    UUID getIdProducto();
    String getProductoCodigo();
    String getProductoNombre();
    String getCategoriaNombre();
    String getDescripcion();
    BigDecimal getPrecio();
    BigDecimal getPrecioFinal();
    BigDecimal getDescuentoPorcentaje();
    Integer getStockDisponible();
}
