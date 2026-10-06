package com.ceramax.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UsuarioUpdateRequest(
    @NotBlank @Size(max = 20) String codigo,
    @NotNull UUID rolId,
    UUID sucursalId,
    @NotBlank @Pattern(regexp = "DNI|RUC|CE|PASAPORTE") String tipoDocumento,
    @NotBlank @Size(max = 20) String numeroDocumento,
    @NotBlank @Size(max = 80) String nombres,
    @NotBlank @Size(max = 80) String apellidos,
    @NotBlank @Email @Size(max = 120) String email,
    @Pattern(regexp = "^$|.{8,100}", message = "La contraseña debe tener entre 8 y 100 caracteres")
    String password,
    @Size(max = 20) String telefono,
    Boolean activo
) {}
