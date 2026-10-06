package com.ceramax.api.dto.response;

public record AuthResponse(
    String token,
    String tipo,
    String email,
    String nombres,
    String apellidos,
    String rol
) {}
