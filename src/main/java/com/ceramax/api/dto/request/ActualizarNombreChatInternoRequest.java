package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarNombreChatInternoRequest(
    @NotBlank @Size(max = 150) String asunto
) {}
