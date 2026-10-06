package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "El email es obligatorio") String email,
    @NotBlank(message = "La contraseña es obligatoria") String password
) {}
