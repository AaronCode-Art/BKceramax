package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record GaleriaImagenRequest(
    @NotBlank(message = "La URL es obligatoria") String url,
    @Size(max = 200) String publicId,
    @PositiveOrZero Short orden,
    Boolean esPrincipal
) {}
