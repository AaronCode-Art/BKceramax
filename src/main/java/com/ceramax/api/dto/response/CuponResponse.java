package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CuponResponse(
    UUID id,
    String codigo,
    String tipoDescuento,
    BigDecimal valor,
    BigDecimal compraMinima,
    Integer usoMaximo,
    Long usos,
    Integer limitePorCliente,
    OffsetDateTime iniciaEn,
    OffsetDateTime venceEn,
    String anuncioTitulo,
    String anuncioMensaje,
    Boolean anunciar,
    Boolean activo
) {}
