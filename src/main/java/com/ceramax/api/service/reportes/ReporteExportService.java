package com.ceramax.api.service.reportes;

import com.ceramax.api.dto.response.ReporteVentasInventarioResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.InventarioReporteResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.PagoResumenResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.ProductoMasVendidoResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.awt.Color;
import org.apache.poi.ss.util.CellRangeAddress;
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
    private static final float MARGEN = 28;
    private static final float TAMANO_TEXTO = 7;
    private static final float ANCHO_TABLA = PDRectangle.LETTER.getWidth() - MARGEN * 2;

    public byte[] generarPdf(ReporteVentasInventarioResponse reporte) {
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            EscritorTablasPdf escritor = new EscritorTablasPdf(documento, normal, negrita);
            escritor.tabla(
                "REPORTE DE VENTAS E INVENTARIO",
                new String[] {"Dato", "Valor"},
                List.of(
                    new String[] {"Periodo de ventas", reporte.desde() + " al " + reporte.hasta()},
                    new String[] {"Inventario actual al", reporte.generadoEn().toString()},
                    new String[] {"Ventas aprobadas", String.valueOf(reporte.cantidadVentas())},
                    new String[] {"Ventas web", String.valueOf(reporte.ventasWeb())},
                    new String[] {"Ventas presenciales", String.valueOf(reporte.ventasPresenciales())},
                    new String[] {"Ingresos totales", "S/ " + reporte.ingresosTotales().toPlainString()}
                ),
                new float[] {1, 2}
            );
            escritor.tabla(
                "PRODUCTOS MAS VENDIDOS",
                new String[] {"Codigo", "Producto", "Unidades vendidas", "Importe vendido"},
                reporte.productosMasVendidos().stream()
                    .map(item -> new String[] {
                        valor(item.codigo()), valor(item.nombre()), valor(item.unidadesVendidas()),
                        "S/ " + valor(item.importeVendido())
                    }).toList(),
                new float[] {1, 3, 1.5f, 1.5f}
            );
            escritor.tabla(
                "RESUMEN DE PAGOS",
                new String[] {"Metodo", "Estado", "Cantidad", "Monto total"},
                reporte.resumenPagos().stream()
                    .map(item -> new String[] {
                        valor(item.metodoPago()), valor(item.estado()), valor(item.cantidadPagos()),
                        "S/ " + valor(item.montoTotal())
                    }).toList(),
                new float[] {1.3f, 1.3f, 1, 1.5f}
            );
            escritor.tabla(
                "INVENTARIO ACTUAL",
                new String[] {
                    "Almacen", "Cod. almacen", "Producto", "Cod. producto", "Categoria",
                    "Fisico", "Reservado", "Disponible", "Minimo", "Stock bajo"
                },
                reporte.inventarioActual().stream()
                    .map(item -> new String[] {
                        valor(item.almacenNombre()), valor(item.almacenCodigo()), valor(item.productoNombre()),
                        valor(item.productoCodigo()), valor(item.categoriaNombre()), valor(item.stockFisico()),
                        valor(item.stockReservado()), valor(item.stockDisponible()), valor(item.stockMinimo()),
                        Boolean.TRUE.equals(item.stockBajo()) ? "Si" : "No"
                    }).toList(),
                new float[] {2, 1.2f, 2.3f, 1.2f, 1.7f, 0.8f, 0.8f, 0.9f, 0.8f, 1}
            );
            escritor.cerrar();
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
            ventas.setAutoFilter(new CellRangeAddress(11, Math.max(11, fila - 1), 0, columnasProductos.length - 1));

            Sheet pagos = libro.createSheet("Pagos");
            pagos.createFreezePane(0, 1);
            String[] columnasPagos = {"Método", "Estado", "Cantidad de pagos", "Monto total"};
            escribirEncabezados(pagos.createRow(0), columnasPagos, encabezado);
            int filaPago = 1;
            for (PagoResumenResponse pago : reporte.resumenPagos()) {
                Row row = pagos.createRow(filaPago++);
                escribirTexto(row, 0, pago.metodoPago(), null);
                escribirTexto(row, 1, pago.estado(), null);
                escribirNumero(row, 2, pago.cantidadPagos());
                escribirMoneda(row, 3, pago.montoTotal(), moneda);
            }
            pagos.setAutoFilter(new CellRangeAddress(0, Math.max(0, filaPago - 1), 0, columnasPagos.length - 1));

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
            inventario.setAutoFilter(
                new CellRangeAddress(0, Math.max(0, filaInventario[0] - 1), 0, columnasInventario.length - 1)
            );

            ventas.setColumnWidth(0, 24 * 256);
            ventas.setColumnWidth(1, 40 * 256);
            ventas.setColumnWidth(2, 20 * 256);
            ventas.setColumnWidth(3, 20 * 256);
            pagos.setColumnWidth(0, 22 * 256);
            pagos.setColumnWidth(1, 18 * 256);
            pagos.setColumnWidth(2, 20 * 256);
            pagos.setColumnWidth(3, 20 * 256);
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

    private static String valor(Object valor) {
        return valor == null ? "-" : valor.toString();
    }

    private PDPage nuevaPagina(PDDocument documento) {
        PDPage pagina = new PDPage(new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth()));
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

    private final class EscritorTablasPdf {
        private static final float ALTURA_MINIMA_FILA = 17;
        private static final float INTERLINEADO_CELDA = 8;
        private static final float TAMANO_TITULO = 9;
        private final PDDocument documento;
        private final PDFont normal;
        private final PDFont negrita;
        private PDPageContentStream contenido;
        private String tituloActual;
        private String[] encabezadosActuales;
        private float[] anchosActuales;
        private float y;

        private EscritorTablasPdf(PDDocument documento, PDFont normal, PDFont negrita) throws IOException {
            this.documento = documento;
            this.normal = normal;
            this.negrita = negrita;
            nuevaPagina();
        }

        private void tabla(String titulo, String[] encabezados, List<String[]> filas, float[] proporciones)
            throws IOException {
            float[] anchos = calcularAnchos(proporciones);
            tituloActual = titulo;
            encabezadosActuales = encabezados;
            anchosActuales = anchos;
            dibujarFila(List.of(titulo), new float[] {ANCHO_TABLA}, true, true, false);
            dibujarFila(List.of(encabezados), anchos, true, false, false);
            List<String[]> filasVisibles = filas.isEmpty()
                ? List.<String[]>of(new String[] {"Sin registros"})
                : filas;
            for (String[] fila : filasVisibles) {
                String[] celdas = fila.length == encabezados.length
                    ? fila
                    : completarFila(fila, encabezados.length);
                dibujarFila(List.of(celdas), anchos, false, false, true);
            }
            y -= 8;
        }

        private void dibujarFila(
            List<String> celdas,
            float[] anchos,
            boolean encabezado,
            boolean titulo,
            boolean repetirEncabezado
        )
            throws IOException {
            PDFont fuente = encabezado ? negrita : normal;
            float tamano = titulo ? TAMANO_TITULO : TAMANO_TEXTO;
            List<List<String>> lineasPorCelda = new java.util.ArrayList<>();
            float altura = ALTURA_MINIMA_FILA;
            for (int indice = 0; indice < celdas.size(); indice++) {
                float anchoTexto = anchos[indice] - 8;
                List<String> lineas = ajustarLinea(celdas.get(indice), fuente, tamano, anchoTexto);
                lineasPorCelda.add(lineas);
                altura = Math.max(altura, lineas.size() * INTERLINEADO_CELDA + 8);
            }
            if (y - altura < MARGEN) {
                nuevaPagina();
                if (repetirEncabezado) {
                    dibujarFila(List.of(tituloActual), new float[] {ANCHO_TABLA}, true, true, false);
                    dibujarFila(List.of(encabezadosActuales), anchosActuales, true, false, false);
                }
            }
            float x = MARGEN;
            for (int indice = 0; indice < celdas.size(); indice++) {
                float ancho = anchos[indice];
                contenido.setNonStrokingColor(encabezado ? new Color(225, 233, 242) : Color.WHITE);
                contenido.setStrokingColor(new Color(160, 170, 180));
                contenido.addRect(x, y - altura, ancho, altura);
                contenido.fillAndStroke();
                contenido.setNonStrokingColor(Color.BLACK);
                List<String> lineas = lineasPorCelda.get(indice);
                float baseline = y - 11;
                for (String linea : lineas) {
                    contenido.beginText();
                    contenido.setFont(fuente, tamano);
                    contenido.newLineAtOffset(x + 4, baseline);
                    contenido.showText(linea);
                    contenido.endText();
                    baseline -= INTERLINEADO_CELDA;
                }
                x += ancho;
            }
            y -= altura;
        }

        private float[] calcularAnchos(float[] proporciones) {
            float total = 0;
            for (float proporcion : proporciones) {
                total += proporcion;
            }
            float[] anchos = new float[proporciones.length];
            for (int indice = 0; indice < proporciones.length; indice++) {
                anchos[indice] = ANCHO_TABLA * proporciones[indice] / total;
            }
            return anchos;
        }

        private String[] completarFila(String[] fila, int cantidadColumnas) {
            String[] resultado = new String[cantidadColumnas];
            java.util.Arrays.fill(resultado, "");
            System.arraycopy(fila, 0, resultado, 0, Math.min(fila.length, cantidadColumnas));
            return resultado;
        }

        private List<String> ajustarLinea(String texto, PDFont font, float size, float maxWidth) throws IOException {
            if (texto.isEmpty()) {
                return List.of("");
            }
            List<String> lineas = new java.util.ArrayList<>();
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

        private void nuevaPagina() throws IOException {
            if (contenido != null) {
                contenido.close();
            }
            PDPage pagina = ReporteExportService.this.nuevaPagina(documento);
            contenido = new PDPageContentStream(documento, pagina);
            y = pagina.getMediaBox().getHeight() - MARGEN;
        }

        private void cerrar() throws IOException {
            if (contenido != null) {
                contenido.close();
                contenido = null;
            }
        }
    }
}
