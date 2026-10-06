package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record EntradaInventarioRequest(
    @NotNull UUID almacenId,
    @NotNull UUID productoId,
    @NotNull @Positive Integer cantidad,
    @Size(max = 300) String observacion
) {}
