package com.ceramax.api.dto.request;

import jakarta.validation.constraints.Size;

public record CierreTrasladoRequest(@Size(max = 250) String observacion) {}
