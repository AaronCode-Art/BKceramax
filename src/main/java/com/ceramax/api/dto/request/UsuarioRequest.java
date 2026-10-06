package com.ceramax.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UsuarioRequest(
    @NotBlank(message = "El código es obligatorio") @Size(max = 8, message = "El código no puede superar 8 caracteres") String codigo,
    @NotNull(message = "El rol es obligatorio") UUID rolId,
    UUID sucursalId,
    @NotBlank(message = "El tipo de documento es obligatorio") String tipoDocumento,
    @NotBlank(message = "El número de documento es obligatorio") String numeroDocumento,
    @NotBlank(message = "Los nombres son obligatorios") String nombres,
    @NotBlank(message = "Los apellidos son obligatorios") String apellidos,
    @NotBlank(message = "El email es obligatorio") @Email(message = "El email no es válido") String email,
    @NotBlank(message = "La contraseña es obligatoria") String password,
    String telefono,
    Boolean activo
) {}
