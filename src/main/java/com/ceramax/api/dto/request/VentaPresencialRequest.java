package com.ceramax.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;
import java.util.UUID;

public record VentaPresencialRequest(
    UUID clienteId,
    @Pattern(regexp = "DELIVERY|RECOJO_TIENDA") String tipoEntrega,
    @Valid DatosClienteVentaRequest datosCliente,
    @NotBlank @Pattern(regexp = "BOLETA|FACTURA") String tipoComprobante,
    @NotBlank @Pattern(regexp = "TARJETA|YAPE|PLIN|TRANSFERENCIA|EFECTIVO") String metodoPago,
    @Pattern(regexp = "[0-9]{11}") String ruc,
    @Size(max = 150) String razonSocial,
    @NotNull @NotEmpty List<@Valid DetallePedidoRequest> detalles
) {
    @AssertTrue(message = "Los datos del cliente deben ser válidos y completos para la venta")
    public boolean isDatosClienteVentaValidos() {
        if (datosCliente == null) return true;
        return datosCliente.tieneDocumentoValido()
            && ("DELIVERY".equals(tipoEntrega)
                ? datosCliente.tieneDatosDeEnvio()
                : "RECOJO_TIENDA".equals(tipoEntrega));
    }
}
