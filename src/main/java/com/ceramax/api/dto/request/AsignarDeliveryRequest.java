package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AsignarDeliveryRequest(@NotNull UUID usuarioId) {}
