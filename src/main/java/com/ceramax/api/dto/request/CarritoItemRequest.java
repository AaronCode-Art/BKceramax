package com.ceramax.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CarritoItemRequest(
    @NotNull UUID productoId,
    @NotNull @Min(1) @Max(1000) Integer cantidad
) {}
