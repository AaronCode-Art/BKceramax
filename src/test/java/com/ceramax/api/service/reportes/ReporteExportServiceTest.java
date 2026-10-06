package com.ceramax.api.service.reportes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ceramax.api.dto.response.ReporteVentasInventarioResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.InventarioReporteResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.ProductoMasVendidoResponse;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ReporteExportServiceTest {

    private final ReporteExportService exportService = new ReporteExportService();

    @Test
    void generaPdfLegibleConMensajeParaPeriodoSinVentas() throws Exception {
        byte[] pdf = exportService.generarPdf(reporteVacio());

        assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF-"));
        try (var documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("REPORTE CONSOLIDADO"));
            assertTrue(texto.contains("No hay ventas aprobadas en el periodo."));
            assertTrue(texto.contains("Inventario actual"));
        }
    }

    @Test
    void generaExcelConHojasDeVentasEInventario() throws Exception {
        byte[] excel = exportService.generarExcel(reporteConInventario());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            assertEquals("Ventas", workbook.getSheetAt(0).getSheetName());
            assertEquals("Inventario actual", workbook.getSheetAt(1).getSheetName());
            assertEquals(
                "Maceta",
                workbook.getSheet("Ventas").getRow(12).getCell(1).getStringCellValue()
            );
            assertEquals(
                "Almacén principal",
                workbook.getSheet("Inventario actual").getRow(1).getCell(0).getStringCellValue()
            );
        }
    }

    @Test
    void generaExcelConInventarioQueSuperaLaVentanaEnMemoria() throws Exception {
        ReporteVentasInventarioResponse reporte = reporteConInventario();
        List<InventarioReporteResponse> inventario = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            inventario.add(new InventarioReporteResponse(
                UUID.randomUUID(),
                "ALM-" + i,
                "Almacén " + i,
                UUID.randomUUID(),
                "PRO-" + i,
                "Producto " + i,
                "Categoría",
                10,
                1,
                9,
                5,
                false
            ));
        }
        ReporteVentasInventarioResponse grande = new ReporteVentasInventarioResponse(
            reporte.desde(),
            reporte.hasta(),
            reporte.generadoEn(),
            reporte.cantidadVentas(),
            reporte.ventasWeb(),
            reporte.ventasPresenciales(),
            reporte.ingresosTotales(),
            reporte.productosMasVendidos(),
            List.of()
        );

        byte[] excel = exportService.generarExcel(grande, escritor -> inventario.forEach(escritor));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            assertEquals(251, workbook.getSheet("Inventario actual").getPhysicalNumberOfRows());
            assertEquals(
                "Producto 249",
                workbook.getSheet("Inventario actual").getRow(250).getCell(2).getStringCellValue()
            );
        }
    }

    private ReporteVentasInventarioResponse reporteVacio() {
        return new ReporteVentasInventarioResponse(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
            OffsetDateTime.now(ZoneOffset.UTC),
            0L,
            0L,
            0L,
            BigDecimal.ZERO,
            List.of(),
            List.of()
        );
    }

    private ReporteVentasInventarioResponse reporteConInventario() {
        return new ReporteVentasInventarioResponse(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
            OffsetDateTime.now(ZoneOffset.UTC),
            1L,
            1L,
            0L,
            new BigDecimal("25.50"),
            List.of(new ProductoMasVendidoResponse(
                UUID.randomUUID(),
                "PRO-0001",
                "Maceta",
                2L,
                new BigDecimal("25.50")
            )),
            List.of(new InventarioReporteResponse(
                UUID.randomUUID(),
                "ALM-0001",
                "Almacén principal",
                UUID.randomUUID(),
                "PRO-0001",
                "Maceta",
                "Decoración",
                10,
                2,
                8,
                5,
                false
            ))
        );
    }
}
