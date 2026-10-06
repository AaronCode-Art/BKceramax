package com.ceramax.api.dto.request;

import jakarta.validation.constraints.Size;

public record CrearChatSoporteRequest(
    @Size(max = 150) String asunto
) {}
