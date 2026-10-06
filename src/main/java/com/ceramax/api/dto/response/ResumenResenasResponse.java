package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ResumenResenasResponse(
    UUID productoId,
    Integer cantidadResenas,
    BigDecimal calificacionPromedio,
    Integer cincoEstrellas,
    Integer cuatroEstrellas,
    Integer tresEstrellas,
    Integer dosEstrellas,
    Integer unaEstrella
) {}
