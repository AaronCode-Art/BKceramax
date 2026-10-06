package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResenaImagenRequest(
    @NotBlank @Size(max = 2048) String url,
    @Size(max = 200) String publicId
) {}
