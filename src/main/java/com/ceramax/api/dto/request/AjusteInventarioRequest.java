package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AjusteInventarioRequest(
    @NotNull UUID almacenId,
    @NotNull UUID productoId,
    @NotNull @PositiveOrZero Integer nuevoStock,
    @NotBlank @Pattern(regexp = "MERMA|ROBO|INVENTARIO_FISICO|CORRECCION|REPOSICION_AUTORIZADA|OTRO") String motivo,
    @NotBlank @Size(max = 300) String observacion
) {}
