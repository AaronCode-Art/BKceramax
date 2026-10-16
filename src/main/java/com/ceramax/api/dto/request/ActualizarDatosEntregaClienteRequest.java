package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarDatosEntregaClienteRequest(
    @NotBlank @Pattern(regexp = "9[0-9]{8}", message = "El teléfono debe empezar con 9 y tener exactamente 9 dígitos")
    String telefono,
    @NotBlank @Size(max = 60) String departamento,
    @NotBlank @Size(max = 60) String provincia,
    @NotBlank @Size(max = 60) String distrito,
    @NotBlank @Size(max = 200) String direccion,
    @Size(max = 10) @Pattern(regexp = "^$|[0-9]+") String codigoPostal,
    @Size(max = 250) String referencia,
    @Pattern(regexp = "^$|[0-9]{6}") String idUbigeo
) {}
