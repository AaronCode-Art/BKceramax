package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record DetallePedidoRequest(
    @NotNull UUID productoId,
    @NotNull @Positive Integer cantidad
) {}
