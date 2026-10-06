package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CrearChatSoporteParaClienteRequest(
    @NotNull UUID clienteId,
    @Size(max = 150) String asunto
) {}
