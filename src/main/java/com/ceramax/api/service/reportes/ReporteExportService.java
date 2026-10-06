package com.ceramax.api.service.reportes;

import com.ceramax.api.dto.response.ReporteVentasInventarioResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.InventarioReporteResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.ProductoMasVendidoResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReporteExportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporteExportService.class);
    private static final int FILAS_EN_MEMORIA_EXCEL = 100;
    private static final float MARGEN = 48;
    private static final float TAMANO_TEXTO = 9;
    private static final float ANCHO_TEXTO = PDRectangle.LETTER.getWidth() - MARGEN * 2;
    private static final float INTERLINEADO = 14;

    public byte[] generarPdf(ReporteVentasInventarioResponse reporte) {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            List<LineaPdf> lineas = construirLineasPdf(reporte);
            PDPage pagina = nuevaPagina(documento);
            float y = pagina.getMediaBox().getHeight() - MARGEN;
            PDPageContentStream contenido = new PDPageContentStream(documento, pagina);
            try {
                for (LineaPdf linea : lineas) {
                    List<String> fragmentos = ajustarLinea(
                        linea.texto(),
                        linea.negrita() ? negrita : normal,
                        TAMANO_TEXTO,
                        ANCHO_TEXTO
                    );
                    for (String fragmento : fragmentos) {
                        if (y < MARGEN) {
                            contenido.close();
                            pagina = nuevaPagina(documento);
                            y = pagina.getMediaBox().getHeight() - MARGEN;
                            contenido = new PDPageContentStream(documento, pagina);
                        }
                        contenido.beginText();
                        contenido.setFont(linea.negrita() ? negrita : normal, TAMANO_TEXTO);
                        contenido.newLineAtOffset(MARGEN, y);
                        contenido.showText(fragmento);
                        contenido.endText();
                        y -= INTERLINEADO;
                    }
                    y -= linea.negrita() ? 4 : 0;
                }
            } finally {
                contenido.close();
            }
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el reporte PDF", exception);
        }
    }

    public byte[] generarExcel(ReporteVentasInventarioResponse reporte) {
        return generarExcel(reporte, escritor -> reporte.inventarioActual().forEach(escritor));
    }

    public byte[] generarExcel(
        ReporteVentasInventarioResponse reporte,
        Consumer<Consumer<InventarioReporteResponse>> escritorInventario
    ) {
        SXSSFWorkbook libro = new SXSSFWorkbook(FILAS_EN_MEMORIA_EXCEL);
        libro.setCompressTempFiles(true);
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle encabezado = crearEstiloEncabezado(libro);
            CellStyle moneda = libro.createCellStyle();
            moneda.setDataFormat(libro.createDataFormat().getFormat("\"S/\" #,##0.00"));

            Sheet ventas = libro.createSheet("Ventas");
            ventas.createFreezePane(0, 1);
            escribirTexto(ventas.createRow(0), 0, "REPORTE CONSOLIDADO DE VENTAS", encabezado);
            escribirTexto(ventas.createRow(1), 0, "Desde", null);
            escribirTexto(ventas.getRow(1), 1, reporte.desde().toString(), null);
            escribirTexto(ventas.createRow(2), 0, "Hasta", null);
            escribirTexto(ventas.getRow(2), 1, reporte.hasta().toString(), null);
            escribirTexto(ventas.createRow(3), 0, "Generado en UTC", null);
            escribirTexto(ventas.getRow(3), 1, reporte.generadoEn().toString(), null);
            escribirTexto(ventas.createRow(5), 0, "Indicador", encabezado);
            escribirTexto(ventas.getRow(5), 1, "Valor", encabezado);
            escribirTexto(ventas.createRow(6), 0, "Ventas aprobadas", null);
            escribirNumero(ventas.getRow(6), 1, reporte.cantidadVentas());
            escribirTexto(ventas.createRow(7), 0, "Ventas web", null);
            escribirNumero(ventas.getRow(7), 1, reporte.ventasWeb());
            escribirTexto(ventas.createRow(8), 0, "Ventas presenciales", null);
            escribirNumero(ventas.getRow(8), 1, reporte.ventasPresenciales());
            escribirTexto(ventas.createRow(9), 0, "Ingresos totales", null);
            escribirMoneda(ventas.getRow(9), 1, reporte.ingresosTotales(), moneda);

            String[] columnasProductos = {"Código", "Producto", "Unidades vendidas", "Importe vendido"};
            Row encabezadoProductos = ventas.createRow(11);
            escribirEncabezados(encabezadoProductos, columnasProductos, encabezado);
            int fila = 12;
            for (ProductoMasVendidoResponse producto : reporte.productosMasVendidos()) {
                Row row = ventas.createRow(fila++);
                escribirTexto(row, 0, producto.codigo(), null);
                escribirTexto(row, 1, producto.nombre(), null);
                escribirNumero(row, 2, producto.unidadesVendidas());
                escribirMoneda(row, 3, producto.importeVendido(), moneda);
            }

            Sheet inventario = libro.createSheet("Inventario actual");
            inventario.createFreezePane(0, 1);
            String[] columnasInventario = {
                "Almacén", "Código almacén", "Producto", "Código producto", "Categoría",
                "Stock físico", "Reservado", "Disponible", "Stock mínimo", "Stock bajo"
            };
            escribirEncabezados(inventario.createRow(0), columnasInventario, encabezado);
            int[] filaInventario = {1};
            escritorInventario.accept(item -> {
                Row row = inventario.createRow(filaInventario[0]++);
                escribirTexto(row, 0, item.almacenNombre(), null);
                escribirTexto(row, 1, item.almacenCodigo(), null);
                escribirTexto(row, 2, item.productoNombre(), null);
                escribirTexto(row, 3, item.productoCodigo(), null);
                escribirTexto(row, 4, item.categoriaNombre(), null);
                escribirNumero(row, 5, item.stockFisico());
                escribirNumero(row, 6, item.stockReservado());
                escribirNumero(row, 7, item.stockDisponible());
                escribirNumero(row, 8, item.stockMinimo());
                escribirTexto(row, 9, Boolean.TRUE.equals(item.stockBajo()) ? "Sí" : "No", null);
            });

            ventas.setColumnWidth(0, 24 * 256);
            ventas.setColumnWidth(1, 40 * 256);
            ventas.setColumnWidth(2, 20 * 256);
            ventas.setColumnWidth(3, 20 * 256);
            int[] anchosInventario = {28, 18, 36, 18, 24, 14, 14, 14, 14, 14};
            for (int columna = 0; columna < anchosInventario.length; columna++) {
                inventario.setColumnWidth(columna, anchosInventario[columna] * 256);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el reporte Excel", exception);
        } finally {
            if (!libro.dispose()) {
                LOGGER.warn("No se pudieron eliminar todos los archivos temporales del reporte Excel");
            }
            try {
                libro.close();
            } catch (IOException exception) {
                throw new IllegalStateException("No se pudo cerrar el reporte Excel", exception);
            }
        }
    }

    private List<LineaPdf> construirLineasPdf(ReporteVentasInventarioResponse reporte) {
        List<LineaPdf> lineas = new ArrayList<>();
        lineas.add(new LineaPdf("REPORTE CONSOLIDADO DE VENTAS E INVENTARIO", true));
        lineas.add(new LineaPdf("Periodo de ventas: " + reporte.desde() + " al " + reporte.hasta(), false));
        lineas.add(new LineaPdf("Inventario: fotografía actual al " + reporte.generadoEn(), false));
        lineas.add(new LineaPdf("", false));
        lineas.add(new LineaPdf("Resumen de ventas", true));
        lineas.add(new LineaPdf("Ventas aprobadas: " + reporte.cantidadVentas(), false));
        lineas.add(new LineaPdf("Ventas web: " + reporte.ventasWeb(), false));
        lineas.add(new LineaPdf("Ventas presenciales: " + reporte.ventasPresenciales(), false));
        lineas.add(new LineaPdf("Ingresos totales: S/ " + reporte.ingresosTotales().toPlainString(), false));
        lineas.add(new LineaPdf("", false));
        lineas.add(new LineaPdf("Productos más vendidos", true));
        if (reporte.productosMasVendidos().isEmpty()) {
            lineas.add(new LineaPdf("No hay ventas aprobadas en el periodo.", false));
        } else {
            int puesto = 1;
            for (ProductoMasVendidoResponse producto : reporte.productosMasVendidos()) {
                lineas.add(new LineaPdf(
                    puesto++ + ". " + producto.codigo() + " - " + producto.nombre()
                        + " | Unidades: " + producto.unidadesVendidas()
                        + " | Importe: S/ " + producto.importeVendido().toPlainString(),
                    false
                ));
            }
        }
        lineas.add(new LineaPdf("", false));
        lineas.add(new LineaPdf("Inventario actual", true));
        if (reporte.inventarioActual().isEmpty()) {
            lineas.add(new LineaPdf("No hay inventario activo para mostrar.", false));
        } else {
            for (InventarioReporteResponse item : reporte.inventarioActual()) {
                lineas.add(new LineaPdf(
                    item.almacenCodigo() + " " + item.almacenNombre()
                        + " | " + item.productoCodigo() + " " + item.productoNombre()
                        + " | Físico: " + item.stockFisico()
                        + " | Reservado: " + item.stockReservado()
                        + " | Disponible: " + item.stockDisponible()
                        + " | Mínimo: " + item.stockMinimo()
                        + " | Bajo: " + (Boolean.TRUE.equals(item.stockBajo()) ? "Sí" : "No"),
                    false
                ));
            }
        }
        return lineas;
    }

    private List<String> ajustarLinea(String texto, PDFont font, float size, float maxWidth) throws IOException {
        if (texto.isEmpty()) {
            return List.of("");
        }
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split("\\s+")) {
            String propuesta = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (!actual.isEmpty() && font.getStringWidth(propuesta) * size / 1000 > maxWidth) {
                lineas.add(actual.toString());
                actual = new StringBuilder(palabra);
            } else {
                actual.setLength(0);
                actual.append(propuesta);
            }
        }
        if (!actual.isEmpty()) {
            lineas.add(actual.toString());
        }
        return lineas;
    }

    private PDPage nuevaPagina(PDDocument documento) {
        PDPage pagina = new PDPage(PDRectangle.LETTER);
        documento.addPage(pagina);
        return pagina;
    }

    private CellStyle crearEstiloEncabezado(Workbook libro) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        CellStyle style = libro.createCellStyle();
        style.setFont(fuente);
        return style;
    }

    private void escribirEncabezados(Row row, String[] encabezados, CellStyle estilo) {
        for (int columna = 0; columna < encabezados.length; columna++) {
            escribirTexto(row, columna, encabezados[columna], estilo);
        }
    }

    private void escribirTexto(Row row, int columna, String valor, CellStyle estilo) {
        Cell cell = row.createCell(columna);
        cell.setCellValue(valor == null ? "" : valor);
        if (estilo != null) {
            cell.setCellStyle(estilo);
        }
    }

    private void escribirNumero(Row row, int columna, Number valor) {
        row.createCell(columna).setCellValue(valor == null ? 0 : valor.doubleValue());
    }

    private void escribirMoneda(Row row, int columna, java.math.BigDecimal valor, CellStyle estilo) {
        Cell cell = row.createCell(columna);
        cell.setCellValue(valor == null ? 0 : valor.doubleValue());
        cell.setCellStyle(estilo);
    }

    private record LineaPdf(String texto, boolean negrita) {}
}
