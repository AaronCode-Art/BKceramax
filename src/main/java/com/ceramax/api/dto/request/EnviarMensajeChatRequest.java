package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeChatRequest(
    @NotBlank @Size(max = 4000) String contenido
) {}
