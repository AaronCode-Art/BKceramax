package com.ceramax.api.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ceramax.api.dto.request.CrearPedidoWebRequest;
import com.ceramax.api.dto.request.DetallePedidoRequest;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.model.acceso.Sucursal;
import com.ceramax.api.model.venta.EstadoEnvio;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PedidoFactoryTest {

    private final PedidoFactory pedidoFactory = new PedidoFactory(new ComprobanteFactory());

    @Test
    void creaPedidoDeliveryConDatosDeEnvioYComprobante() {
        Cliente cliente = new Cliente();
        cliente.setTipoDocumento("DNI");
        cliente.setNumeroDocumento("12345678");
        cliente.setNombres("Ana");
        cliente.setApellidos("Pérez");
        cliente.setEmail("ana@example.com");
        Sucursal sucursal = new Sucursal();
        sucursal.setNombre("Principal");
        sucursal.setDireccion("Av. Central 123");
        EstadoEnvio estado = new EstadoEnvio();
        estado.setCodigo("PENDIENTE_PAGO");
        estado.setNombre("Pendiente de pago");

        CrearPedidoWebRequest request = new CrearPedidoWebRequest(
            "DELIVERY",
            "FACTURA",
            "20123456789",
            "Cerámicas Ana S.A.C.",
            "Lima",
            "Lima",
            "Miraflores",
            "Av. Ejemplo 456",
            null,
            null,
            null,
            "SIMULADO",
            "TARJETA",
            List.of(new DetallePedidoRequest(UUID.randomUUID(), 2))
        );
        var pedido = pedidoFactory.crearPedidoWeb(
            request,
            cliente,
            sucursal,
            estado,
            new BigDecimal("100.00"),
            new BigDecimal("15.00"),
            new BigDecimal("18.00"),
            new BigDecimal("133.00")
        );

        assertEquals("WEB", pedido.getCanal());
        assertEquals("DELIVERY", pedido.getTipoEntrega());
        assertEquals("20123456789", pedido.getRuc());
        assertEquals("Cerámicas Ana S.A.C.", pedido.getRazonSocial());
        assertEquals(new BigDecimal("133.00"), pedido.getTotal());
    }

    @Test
    void rechazaDeliverySinDireccion() {
        CrearPedidoWebRequest request = new CrearPedidoWebRequest(
            "DELIVERY",
            "BOLETA",
            null,
            null,
            null,
            null,
            "Miraflores",
            null,
            null,
            null,
            null,
            "SIMULADO",
            "YAPE",
            List.of(new DetallePedidoRequest(UUID.randomUUID(), 1))
        );

        assertThrows(
            BusinessException.class,
            () -> pedidoFactory.crearPedidoWeb(
                request,
                new Cliente(),
                new Sucursal(),
                new EstadoEnvio(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
            )
        );
    }

    @Test
    void boletaNoIncluyeDatosDeFactura() {
        var comprobante = new ComprobanteFactory().crear("BOLETA", "20123456789", "Empresa S.A.");

        assertEquals("BOLETA", comprobante.tipo());
        assertNull(comprobante.ruc());
        assertNull(comprobante.razonSocial());
    }
}
