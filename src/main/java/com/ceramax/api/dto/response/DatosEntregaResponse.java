package com.ceramax.api.dto.response;

public record DatosEntregaResponse(
    String destinatario,
    String telefono,
    String departamento,
    String provincia,
    String distrito,
    String direccion,
    String codigoPostal,
    String referencia,
    String sucursalNombre,
    String sucursalDireccion
) {}
