package com.ceramax.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteUpdateRequest(
    @NotBlank @Pattern(regexp = "DNI|RUC|CE|PASAPORTE") String tipoDocumento,
    @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9]+") String numeroDocumento,
    @NotBlank @Size(max = 80) @Pattern(regexp = "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$") String nombres,
    @NotBlank @Size(max = 80) @Pattern(regexp = "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$") String apellidos,
    @NotBlank @Email @Size(max = 120) String email,
    @Pattern(regexp = "^$|.{8,100}", message = "La contraseña debe tener entre 8 y 100 caracteres")
    String password,
    @NotBlank @Pattern(regexp = "9[0-9]{8}", message = "El teléfono debe empezar con 9 y tener exactamente 9 dígitos") String telefono,
    @NotBlank @Size(max = 60) String departamento,
    @NotBlank @Size(max = 60) String provincia,
    @NotBlank @Size(max = 60) String distrito,
    @NotBlank @Size(max = 200) String direccion,
    @NotBlank @Size(max = 10) @Pattern(regexp = "[0-9]+") String codigoPostal,
    @NotBlank @Size(max = 250) String referencia,
    @NotBlank @Pattern(regexp = "[0-9]{6}") String idUbigeo,
    Boolean activo
) {}
