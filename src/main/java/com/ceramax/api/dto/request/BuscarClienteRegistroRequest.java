package com.ceramax.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BuscarClienteRegistroRequest(
    @NotBlank @Pattern(regexp = "DNI|RUC|CE|PASAPORTE") String tipoDocumento,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9]+") String numeroDocumento
) {
    @AssertTrue(message = "El número de documento no corresponde al tipo seleccionado")
    public boolean isDocumentoValido() {
        if (tipoDocumento == null || numeroDocumento == null) return true;
        return switch (tipoDocumento) {
            case "DNI" -> numeroDocumento.matches("[0-9]{8}");
            case "RUC" -> numeroDocumento.matches("[0-9]{11}");
            case "CE", "PASAPORTE" -> numeroDocumento.matches("[A-Za-z0-9]{1,20}");
            default -> false;
        };
    }
}
