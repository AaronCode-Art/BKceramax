package com.ceramax.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CuponRequest(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,20}") String codigo,
    @NotBlank @Pattern(regexp = "PORCENTAJE|MONTO_FIJO") String tipoDescuento,
    @NotNull @DecimalMin(value = "0.01") BigDecimal valor,
    @NotNull @DecimalMin(value = "0.00") BigDecimal compraMinima,
    @DecimalMin(value = "1") Integer usoMaximo,
    @NotNull @DecimalMin(value = "1") Integer limitePorCliente,
    @NotNull OffsetDateTime iniciaEn,
    OffsetDateTime venceEn,
    @NotBlank @Size(max = 200) String descripcion,
    @NotNull Boolean activo
) {}
