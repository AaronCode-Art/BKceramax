package com.ceramax.api.service.venta;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.response.DetallePedidoResponse;
import com.ceramax.api.dto.response.DatosEntregaResponse;
import com.ceramax.api.dto.response.PagoResponse;
import com.ceramax.api.dto.response.PedidoComprobanteResponse;
import com.ceramax.api.dto.response.PedidoResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class ComprobantePdfServiceTest {

    private final PedidoService pedidoService = mock(PedidoService.class);
    private final ComprobantePdfService comprobantePdfService = new ComprobantePdfService(pedidoService);

    @Test
    void generaPdfImprimibleConDatosDelPedidoProductosYPagoAprobado() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(pedidoService.obtenerDatosComprobante(pedidoId)).thenReturn(comprobante(pedidoId));

        byte[] pdf = comprobantePdfService.generar(pedidoId);

        assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF-"));
        verify(pedidoService).obtenerDatosComprobante(pedidoId);
        try (var documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("COMPROBANTE DE PAGO"));
            assertTrue(texto.contains("BOLETA DE VENTA"));
            assertTrue(texto.contains("B001-00001234"));
            assertTrue(texto.contains("DATOS DEL CLIENTE"));
            assertTrue(texto.contains("DATOS DEL PEDIDO"));
            assertTrue(texto.contains("DESCRIPCION"));
            assertTrue(texto.contains("Maceta decorativa"));
            assertTrue(texto.contains("Tarjeta - APROBADO"));
            assertTrue(texto.contains("TOTAL"));
            assertTrue(texto.contains("S/ 59.00"));
            assertTrue(texto.contains("Documento informativo. No reemplaza un comprobante tributario"));
        }
    }

    @Test
    void paginaElDetalleYNumeraLasPaginasCuandoHayMuchosProductos() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        PedidoComprobanteResponse base = comprobante(pedidoId);
        PedidoResponse pedido = base.pedido();
        List<DetallePedidoResponse> detalles = java.util.stream.IntStream.range(0, 30)
            .mapToObj(indice -> pedido.detalles().getFirst())
            .toList();
        PedidoResponse pedidoConMuchosProductos = new PedidoResponse(
            pedido.id(),
            pedido.codigo(),
            pedido.canal(),
            pedido.tipoEntrega(),
            pedido.estadoCodigo(),
            pedido.estadoNombre(),
            pedido.logisticaId(),
            pedido.logisticaNombre(),
            pedido.deliveryId(),
            pedido.deliveryNombre(),
            pedido.tipoComprobante(),
            pedido.serie(),
            pedido.numeroComprobante(),
            pedido.subtotal(),
            pedido.descuentoCupon(),
            pedido.costoEnvio(),
            pedido.igv(),
            pedido.total(),
            pedido.tieneReserva(),
            pedido.despachado(),
            pedido.createdAt(),
            detalles,
            pedido.pagos(),
            pedido.datosEntrega()
        );
        when(pedidoService.obtenerDatosComprobante(pedidoId)).thenReturn(new PedidoComprobanteResponse(
            pedidoConMuchosProductos,
            base.clienteNombre(),
            base.clienteTipoDocumento(),
            base.clienteNumeroDocumento(),
            base.clienteEmail(),
            base.ruc(),
            base.razonSocial(),
            base.sucursalNombre(),
            base.sucursalDireccion(),
            base.fechaEmision()
        ));

        byte[] pdf = comprobantePdfService.generar(pedidoId);

        try (var documento = Loader.loadPDF(pdf)) {
            assertTrue(documento.getNumberOfPages() > 1);
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Página 1 de"));
            assertTrue(texto.contains("Página " + documento.getNumberOfPages() + " de " + documento.getNumberOfPages()));
        }
    }

    private PedidoComprobanteResponse comprobante(UUID pedidoId) {
        PedidoResponse pedido = new PedidoResponse(
            pedidoId,
            "PED-00001234",
            "PRESENCIAL",
            "TIENDA",
            "COMPLETADA",
            "Completada",
            null,
            null,
            null,
            null,
            "BOLETA",
            "B001",
            "00001234",
            new BigDecimal("50.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new BigDecimal("9.00"),
            new BigDecimal("59.00"),
            false,
            true,
            OffsetDateTime.of(2026, 10, 4, 18, 0, 0, 0, ZoneOffset.ofHours(-5)),
            List.of(new DetallePedidoResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PRO-0001",
                "Maceta decorativa",
                "Decoración",
                null,
                2,
                new BigDecimal("25.00"),
                BigDecimal.ZERO,
                new BigDecimal("50.00"),
                "DESCONTADO"
            )),
            List.of(new PagoResponse(
                UUID.randomUUID(),
                "PAG-00001234",
                "Tarjeta",
                "APROBADO",
                new BigDecimal("59.00"),
                OffsetDateTime.of(2026, 10, 4, 18, 0, 0, 0, ZoneOffset.ofHours(-5))
            )),
            new DatosEntregaResponse(
                "María Cerámica",
                "999111222",
                "Lima",
                "Lima",
                "Miraflores",
                "Av. Principal 123",
                "15074",
                "Casa azul",
                "Sucursal central",
                "Av. Principal 123"
            )
        );
        return new PedidoComprobanteResponse(
            pedido,
            "María Cerámica",
            "DNI",
            "12345678",
            "maria@example.com",
            null,
            null,
            "Sucursal central",
            "Av. Principal 123",
            OffsetDateTime.of(2026, 10, 4, 18, 0, 0, 0, ZoneOffset.ofHours(-5))
        );
    }
}
