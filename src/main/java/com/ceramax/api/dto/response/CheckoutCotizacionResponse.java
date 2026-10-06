package com.ceramax.api.dto.response;

import java.math.BigDecimal;

public record CheckoutCotizacionResponse(
    BigDecimal subtotal,
    BigDecimal igv,
    BigDecimal costoEnvio,
    BigDecimal total
) {}
