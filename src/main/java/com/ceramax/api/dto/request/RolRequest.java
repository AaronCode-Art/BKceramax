package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RolRequest(
    @NotBlank(message = "El código es obligatorio") String codigo,
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    String descripcion
) {}
