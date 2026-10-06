package com.ceramax.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarCarritoItemRequest(@NotNull @Min(1) @Max(1000) Integer cantidad) {}
