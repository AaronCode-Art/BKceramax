package com.ceramax.api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PagoResponse(
    UUID id,
    String codigo,
    String metodoPago,
    String estado,
    BigDecimal monto,
    OffsetDateTime fechaPago
) {}
