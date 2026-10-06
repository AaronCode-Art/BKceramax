package com.ceramax.api.dto.request;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CrearChatInternoRequest(
    UUID usuarioDestinoId,
    @Size(max = 150) String asunto
) {}
