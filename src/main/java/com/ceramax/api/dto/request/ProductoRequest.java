package com.ceramax.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductoRequest(
    @NotBlank(message = "El código es obligatorio") @Size(max = 30) String codigo,
    @NotNull(message = "La categoría es obligatoria") UUID categoriaId,
    @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
    String descripcion,
    @NotNull(message = "Las especificaciones son obligatorias") List<Map<String, Object>> especificaciones,
    @NotNull(message = "El precio es obligatorio") @DecimalMin(value = "0.00") @Digits(integer = 8, fraction = 2) BigDecimal precio,
    @DecimalMin(value = "0.00") @DecimalMax(value = "100.00") @Digits(integer = 3, fraction = 2)
    BigDecimal descuentoPorcentaje,
    Boolean activo
) {}
