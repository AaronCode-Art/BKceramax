package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CotizarCheckoutWebRequest(
    @NotBlank @Pattern(regexp = "DELIVERY|RECOJO_TIENDA") String tipoEntrega
) {}
