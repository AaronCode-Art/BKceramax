package com.ceramax.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CrearPedidoWebRequest(
    @NotBlank @Pattern(regexp = "DELIVERY|RECOJO_TIENDA") String tipoEntrega,
    @NotBlank @Pattern(regexp = "BOLETA|FACTURA") String tipoComprobante,
    @Pattern(regexp = "[0-9]{11}") String ruc,
    @Size(max = 150) String razonSocial,
    @Size(max = 60) String envioDepartamento,
    @Size(max = 60) String envioProvincia,
    @Size(max = 60) String envioDistrito,
    @Size(max = 200) String envioDireccion,
    @Size(max = 10) String envioCodigoPostal,
    @Size(max = 250) String envioReferencia,
    @Pattern(regexp = "[0-9]{6}") String envioIdUbigeo,
    @NotBlank @Pattern(regexp = "SIMULADO") String metodoPago,
    @NotBlank @Pattern(regexp = "YAPE|PLIN|TARJETA") String medioPagoSimulado,
    @NotNull @NotEmpty List<@Valid DetallePedidoRequest> detalles
) {}
