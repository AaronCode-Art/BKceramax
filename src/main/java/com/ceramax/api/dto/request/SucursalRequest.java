package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SucursalRequest(
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    @NotBlank(message = "El departamento es obligatorio") String departamento,
    @NotBlank(message = "La provincia es obligatoria") String provincia,
    @NotBlank(message = "El distrito es obligatorio") String distrito,
    @NotBlank(message = "La dirección es obligatoria") String direccion,
    String referencia,
    String codigoPostal,
    String idUbigeo,
    Boolean activo
) {}
