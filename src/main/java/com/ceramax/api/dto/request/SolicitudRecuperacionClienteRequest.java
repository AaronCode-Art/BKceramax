package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudRecuperacionClienteRequest(
    @NotBlank @Size(max = 120) String identificador
) {}
