package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoPedidoRequest(@NotBlank String estadoCodigo) {}
