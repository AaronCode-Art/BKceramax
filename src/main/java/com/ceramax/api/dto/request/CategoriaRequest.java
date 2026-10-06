package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
    @NotBlank(message = "El código es obligatorio") @Size(max = 20) String codigo,
    @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
    @Size(max = 250) String descripcion,
    Boolean activo
) {}
