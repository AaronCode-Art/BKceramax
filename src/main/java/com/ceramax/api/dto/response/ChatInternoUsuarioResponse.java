package com.ceramax.api.dto.response;

import java.util.UUID;

public record ChatInternoUsuarioResponse(
    UUID id,
    String nombres,
    String apellidos,
    String email,
    String rol
) {}
