package com.ceramax.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record VentaPresencialRequest(
    UUID clienteId,
    @NotBlank @Pattern(regexp = "BOLETA|FACTURA") String tipoComprobante,
    @NotBlank @Pattern(regexp = "TARJETA|YAPE|PLIN|TRANSFERENCIA|EFECTIVO") String metodoPago,
    @Pattern(regexp = "[0-9]{11}") String ruc,
    @Size(max = 150) String razonSocial,
    @NotNull @NotEmpty List<@Valid DetallePedidoRequest> detalles
) {}
