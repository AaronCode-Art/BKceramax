package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TrasladoInventarioRequest(
    @NotNull UUID almacenOrigenId,
    @NotNull UUID almacenDestinoId,
    @NotNull UUID productoId,
    @NotNull @Positive Integer cantidad,
    @NotNull @Pattern(regexp = "PARA_CLIENTE|REABASTECIMIENTO") String tipoTraslado,
    UUID pedidoId,
    UUID detallePedidoId,
    @Size(max = 250) String observacion
) {}
