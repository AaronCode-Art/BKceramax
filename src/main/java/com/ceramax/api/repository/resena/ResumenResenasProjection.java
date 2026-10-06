package com.ceramax.api.repository.resena;

import java.math.BigDecimal;
import java.util.UUID;

public interface ResumenResenasProjection {
    UUID getProductoId();
    Integer getCantidadResenas();
    BigDecimal getCalificacionPromedio();
    Integer getCincoEstrellas();
    Integer getCuatroEstrellas();
    Integer getTresEstrellas();
    Integer getDosEstrellas();
    Integer getUnaEstrella();
}
