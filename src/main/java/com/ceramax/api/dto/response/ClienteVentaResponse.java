package com.ceramax.api.dto.response;

public record ClienteVentaResponse(
    String tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidos,
    String telefono,
    String departamento,
    String provincia,
    String distrito,
    String direccion,
    String codigoPostal,
    String referencia,
    String idUbigeo
) {}
