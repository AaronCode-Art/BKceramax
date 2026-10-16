package com.ceramax.api.service.reportes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ceramax.api.dto.response.ReporteVentasInventarioResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.InventarioReporteResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.ProductoMasVendidoResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReporteExportBenchmarkTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporteExportBenchmarkTest.class);
    private static final int FILAS_SINTETICAS = 2_000;

    private final ReporteExportService exportService = new ReporteExportService();

    @Test
    @EnabledIfSystemProperty(named = "report.benchmark", matches = "true")
    void mideGeneracionDeExportacionesConDatosSinteticos() {
        ReporteVentasInventarioResponse reporte = reporteSintetico();

        long inicioPdf = System.nanoTime();
        byte[] pdf = exportService.generarPdf(reporte);
        long duracionPdf = System.nanoTime() - inicioPdf;

        long inicioExcel = System.nanoTime();
        byte[] excel = exportService.generarExcel(reporte);
        long duracionExcel = System.nanoTime() - inicioExcel;

        LOGGER.info(
            "Benchmark sintético: {} filas inventario; PDF={} ms/{} bytes; Excel={} ms/{} bytes",
            FILAS_SINTETICAS,
            duracionPdf / 1_000_000,
            pdf.length,
            duracionExcel / 1_000_000,
            excel.length
        );
        assertTrue(pdf.length > 0);
        assertTrue(excel.length > 0);
    }

    private ReporteVentasInventarioResponse reporteSintetico() {
        List<ProductoMasVendidoResponse> productos = new ArrayList<>(FILAS_SINTETICAS);
        List<InventarioReporteResponse> inventario = new ArrayList<>(FILAS_SINTETICAS);
        for (int i = 0; i < FILAS_SINTETICAS; i++) {
            String codigo = "PRO-" + i;
            UUID productoId = UUID.randomUUID();
            productos.add(new ProductoMasVendidoResponse(
                productoId,
                codigo,
                "Producto de prueba " + i,
                10L,
                new BigDecimal("250.00")
            ));
            inventario.add(new InventarioReporteResponse(
                UUID.randomUUID(),
                "ALM-001",
                "Almacén principal",
                productoId,
                codigo,
                "Producto de prueba " + i,
                "Categoría de prueba",
                100,
                10,
                90,
                5,
                false
            ));
        }
        return new ReporteVentasInventarioResponse(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 31),
            OffsetDateTime.now(ZoneOffset.UTC),
            (long) FILAS_SINTETICAS,
            (long) FILAS_SINTETICAS,
            0L,
            new BigDecimal("500000.00"),
            productos,
            List.of(),
            inventario
        );
    }
}
