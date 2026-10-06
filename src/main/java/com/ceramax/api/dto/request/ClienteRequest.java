package com.ceramax.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
    @NotBlank(message = "El tipo de documento es obligatorio")
    @Pattern(regexp = "DNI|RUC|CE|PASAPORTE", message = "El tipo de documento no es válido")
    String tipoDocumento,
    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento no puede superar 20 caracteres")
    @Pattern(regexp = "[A-Za-z0-9]+", message = "El documento solo puede contener letras y números")
    String numeroDocumento,
    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 80, message = "Los nombres no pueden superar 80 caracteres")
    @Pattern(regexp = "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$", message = "Los nombres solo pueden contener letras, espacios, apóstrofes o guiones")
    String nombres,
    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 80, message = "Los apellidos no pueden superar 80 caracteres")
    @Pattern(regexp = "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$", message = "Los apellidos solo pueden contener letras, espacios, apóstrofes o guiones")
    String apellidos,
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es válido")
    @Size(max = 120, message = "El email no puede superar 120 caracteres")
    String email,
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    String password,
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "9[0-9]{8}", message = "El teléfono debe empezar con 9 y tener exactamente 9 dígitos")
    String telefono,
    @NotBlank(message = "El departamento es obligatorio")
    @Size(max = 60, message = "El departamento no puede superar 60 caracteres")
    String departamento,
    @NotBlank(message = "La provincia es obligatoria")
    @Size(max = 60, message = "La provincia no puede superar 60 caracteres")
    String provincia,
    @NotBlank(message = "El distrito es obligatorio")
    @Size(max = 60, message = "El distrito no puede superar 60 caracteres")
    String distrito,
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
    String direccion,
    @NotBlank(message = "El código postal es obligatorio")
    @Size(max = 10, message = "El código postal no puede superar 10 dígitos")
    @Pattern(regexp = "[0-9]+", message = "El código postal solo puede contener números")
    String codigoPostal,
    @NotBlank(message = "La referencia es obligatoria")
    @Size(max = 250, message = "La referencia no puede superar 250 caracteres")
    String referencia,
    @NotBlank(message = "El UBIGEO es obligatorio")
    @Pattern(regexp = "[0-9]{6}", message = "El UBIGEO debe contener exactamente 6 dígitos")
    String idUbigeo
) {
    @AssertTrue(message = "El número de documento no corresponde al tipo seleccionado")
    public boolean isDocumentoValido() {
        if (tipoDocumento == null || numeroDocumento == null) {
            return true;
        }
        return switch (tipoDocumento) {
            case "DNI" -> numeroDocumento.matches("[0-9]{8}");
            case "RUC" -> numeroDocumento.matches("[0-9]{11}");
            case "CE", "PASAPORTE" -> numeroDocumento.matches("[A-Za-z0-9]{1,20}");
            default -> false;
        };
    }
}
