package com.ceramax.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ActualizarResenaRequest(
    @NotNull UUID pedidoId,
    @NotNull @Min(1) @Max(5) Short calificacion,
    String comentario,
    List<@Valid ResenaImagenRequest> imagenes
) {}
