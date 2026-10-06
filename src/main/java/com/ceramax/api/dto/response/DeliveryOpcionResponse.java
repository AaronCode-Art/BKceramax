package com.ceramax.api.dto.response;

import java.util.UUID;

public record DeliveryOpcionResponse(
    UUID id,
    String nombres,
    String apellidos,
    String telefono
) {}
