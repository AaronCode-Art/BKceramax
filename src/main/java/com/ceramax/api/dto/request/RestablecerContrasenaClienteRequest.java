package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

public record RestablecerContrasenaClienteRequest(
    @NotBlank @Size(max = 120) String identificador,
    @NotBlank @Pattern(regexp = "[0-9]{6}") String codigo,
    @NotBlank
    @Size(min = 6, max = 100)
    String nuevaContrasena
) {
    @AssertTrue(message = "La contraseña necesita 6 dígitos, una mayúscula y un símbolo, sin secuencias numéricas consecutivas")
    public boolean isContrasenaValida() {
        return ContrasenaClienteValidator.esValida(nuevaContrasena);
    }
}
