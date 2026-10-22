package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CuponAnuncioResponse(
    String codigo,
    String tipoDescuento,
    BigDecimal valor,
    BigDecimal compraMinima,
    OffsetDateTime venceEn,
    String titulo,
    String mensaje
) {}
