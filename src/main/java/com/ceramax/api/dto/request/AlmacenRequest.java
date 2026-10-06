package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AlmacenRequest(
    @NotBlank @Size(max = 80) String nombre,
    @NotNull UUID sucursalId,
    Boolean activo
) {}
