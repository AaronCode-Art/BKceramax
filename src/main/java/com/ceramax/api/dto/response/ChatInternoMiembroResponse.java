package com.ceramax.api.dto.response;

import java.util.UUID;

public record ChatInternoMiembroResponse(
    UUID chatId,
    UUID usuarioId,
    String nombres,
    String apellidos,
    String email,
    String rol
) {}
