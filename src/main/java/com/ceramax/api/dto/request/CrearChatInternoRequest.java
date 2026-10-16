package com.ceramax.api.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CrearChatInternoRequest(
    UUID usuarioDestinoId,
    @Size(max = 150) String asunto,
    @Size(max = 50) List<@NotNull UUID> miembroIds
) {}
