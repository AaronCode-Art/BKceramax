package com.ceramax.api.dto.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class VentaPresencialRequestTest {

    @Test
    void recojoEnTiendaSoloExigeDatosBasicosDelCliente() {
        DatosClienteVentaRequest cliente = clienteBasico();
        VentaPresencialRequest request = request("RECOJO_TIENDA", cliente);

        assertTrue(request.isDatosClienteVentaValidos());
    }

    @Test
    void deliveryExigeDireccionYUbigeo() {
        VentaPresencialRequest incompleto = request("DELIVERY", clienteBasico());
        DatosClienteVentaRequest clienteCompleto = new DatosClienteVentaRequest(
            "DNI", "12345678", "Ana", "Pérez", "987654321",
            "Lima", "Lima", "Miraflores", "Av. Central 100", "15074", "Puerta negra", "150122"
        );
        VentaPresencialRequest completo = request("DELIVERY", clienteCompleto);

        assertFalse(incompleto.isDatosClienteVentaValidos());
        assertTrue(completo.isDatosClienteVentaValidos());
    }

    private static VentaPresencialRequest request(String tipoEntrega, DatosClienteVentaRequest cliente) {
        return new VentaPresencialRequest(
            null,
            tipoEntrega,
            cliente,
            "BOLETA",
            "EFECTIVO",
            null,
            null,
            List.of(new DetallePedidoRequest(java.util.UUID.randomUUID(), 1))
        );
    }

    private static DatosClienteVentaRequest clienteBasico() {
        return new DatosClienteVentaRequest(
            "DNI", "12345678", "Ana", "Pérez", "987654321",
            "", "", "", "", "", "", ""
        );
    }
}
