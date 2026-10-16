package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DatosClienteVentaRequest(
    @NotBlank @Pattern(regexp = "DNI|RUC|CE|PASAPORTE") String tipoDocumento,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9]+") String numeroDocumento,
    @NotBlank @Size(max = 80) @Pattern(regexp = "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$") String nombres,
    @NotBlank @Size(max = 80) @Pattern(regexp = "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$") String apellidos,
    @NotBlank @Pattern(regexp = "9[0-9]{8}") String telefono,
    @Size(max = 60) String departamento,
    @Size(max = 60) String provincia,
    @Size(max = 60) String distrito,
    @Size(max = 200) String direccion,
    @Size(max = 10) @Pattern(regexp = "[0-9]*") String codigoPostal,
    @Size(max = 250) String referencia,
    @Pattern(regexp = "^$|[0-9]{6}") String idUbigeo
) {
    public boolean tieneDocumentoValido() {
        if (tipoDocumento == null || numeroDocumento == null) return false;
        return switch (tipoDocumento) {
            case "DNI" -> numeroDocumento.matches("[0-9]{8}");
            case "RUC" -> numeroDocumento.matches("[0-9]{11}");
            case "CE", "PASAPORTE" -> numeroDocumento.matches("[A-Za-z0-9]{1,20}");
            default -> false;
        };
    }

    public boolean tieneDatosDeEnvio() {
        return noVacio(departamento)
            && noVacio(provincia)
            && noVacio(distrito)
            && noVacio(direccion)
            && noVacio(codigoPostal)
            && noVacio(referencia)
            && idUbigeo != null
            && idUbigeo.matches("[0-9]{6}");
    }

    private static boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }
}
