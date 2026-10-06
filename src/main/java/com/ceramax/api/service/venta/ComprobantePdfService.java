package com.ceramax.api.service.venta;

import com.ceramax.api.dto.response.DetallePedidoResponse;
import com.ceramax.api.dto.response.PedidoComprobanteResponse;
import com.ceramax.api.dto.response.PedidoResponse;
import com.ceramax.api.exception.BusinessException;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
public class ComprobantePdfService {

    private static final float MARGIN = 42;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;
    private static final float TABLE_BOTTOM = PAGE_HEIGHT - 200;
    private static final Color INK = new Color(36, 33, 31);
    private static final Color MUTED = new Color(112, 107, 101);
    private static final Color TERRACOTTA = new Color(166, 82, 56);
    private static final Color PALE = new Color(246, 244, 240);
    private static final Color RULE = new Color(226, 222, 215);
    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm XXX");

    private final PedidoService pedidoService;

    public ComprobantePdfService(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    public byte[] generar(UUID pedidoId) {
        PedidoComprobanteResponse comprobante = pedidoService.obtenerDatosComprobante(pedidoId);
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            List<PdfPage> paginas = new ArrayList<>();

            PdfPage pagina = nuevaPagina(documento, paginas, normal, negrita, false);
            dibujarDatosCliente(pagina, comprobante, normal, negrita);
            pagina.y = dibujarEncabezadoTabla(pagina, normal, negrita, pagina.y);

            for (int indiceDetalle = 0; indiceDetalle < comprobante.pedido().detalles().size(); indiceDetalle++) {
                DetallePedidoResponse detalle = comprobante.pedido().detalles().get(indiceDetalle);
                List<String> nombre = ajustarTexto(valor(detalle.productoNombre(), "Producto"), normal, 9, 260);
                float altoFila = Math.max(34, nombre.size() * 12 + 18);
                if (pagina.y + altoFila > TABLE_BOTTOM) {
                    pagina = nuevaPagina(documento, paginas, normal, negrita, true);
                    pagina.y = dibujarEncabezadoTabla(pagina, normal, negrita, pagina.y);
                }
                dibujarFila(pagina, indiceDetalle + 1, detalle, nombre, normal, negrita, altoFila);
                pagina.y += altoFila;
            }

            if (pagina.y + 19 + 137 > PAGE_HEIGHT - 60) {
                pagina = nuevaPagina(documento, paginas, normal, negrita, true);
            }
            dibujarTotalesYEntrega(pagina, comprobante, normal, negrita);

            for (int indice = 0; indice < paginas.size(); indice++) {
                PdfPage paginaPdf = paginas.get(indice);
                paginaPdf.contenido.close();
                dibujarPie(documento, paginaPdf.pagina, normal, negrita, paginas.size(), indice + 1);
            }
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el comprobante PDF", exception);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("El comprobante contiene caracteres que no se pueden imprimir en PDF");
        }
    }

    private PdfPage nuevaPagina(
        PDDocument documento,
        List<PdfPage> paginas,
        PDFont normal,
        PDFont negrita,
        boolean continuacion
    ) throws IOException {
        PDPage pagina = new PDPage(PDRectangle.A4);
        documento.addPage(pagina);
        PdfPage lienzo = new PdfPage(pagina, new PDPageContentStream(documento, pagina));
        paginas.add(lienzo);
        dibujarMarca(lienzo, normal, negrita, continuacion);
        return lienzo;
    }

    private void dibujarMarca(PdfPage pagina, PDFont normal, PDFont negrita, boolean continuacion) throws IOException {
        caja(pagina.contenido, 0, 0, PAGE_WIDTH, 104, INK);
        texto(pagina.contenido, "CERAMAX", MARGIN, 24, negrita, 23, Color.WHITE);
        texto(pagina.contenido, "MATERIALES PARA VIVIR CADA ESPACIO", MARGIN + 1, 55, normal, 8, new Color(224, 220, 214));
        texto(pagina.contenido, continuacion ? "DETALLE DEL COMPROBANTE" : "COMPROBANTE DE PAGO", MARGIN, 77, negrita, 10, Color.WHITE);
        pagina.y = 124;
    }

    private void dibujarDatosCliente(
        PdfPage pagina,
        PedidoComprobanteResponse comprobante,
        PDFont normal,
        PDFont negrita
    ) throws IOException {
        PedidoResponse pedido = comprobante.pedido();
        String tipo = tipoComprobante(pedido.tipoComprobante());
        String numero = pedido.serie() != null && pedido.numeroComprobante() != null
            ? pedido.serie() + "-" + pedido.numeroComprobante()
            : pedido.codigo();
        float cajaX = PAGE_WIDTH - MARGIN - 182;
        caja(pagina.contenido, cajaX, 18, 182, 66, Color.WHITE);
        texto(pagina.contenido, tipo, cajaX + 12, 28, negrita, 9, TERRACOTTA);
        texto(pagina.contenido, numero, cajaX + 12, 50, negrita, 14, INK);
        texto(pagina.contenido, "Representación informativa", cajaX + 12, 70, normal, 7, MUTED);

        texto(pagina.contenido, "DATOS DEL CLIENTE", MARGIN, pagina.y, negrita, 8, TERRACOTTA);
        texto(pagina.contenido, "DATOS DEL PEDIDO", PAGE_WIDTH / 2 + 4, pagina.y, negrita, 8, TERRACOTTA);
        float detalleY = pagina.y + 18;
        texto(pagina.contenido, valor(comprobante.razonSocial(), comprobante.clienteNombre()), MARGIN, detalleY, negrita, 10, INK);
        texto(pagina.contenido, "Pedido: " + valor(pedido.codigo(), "-"), PAGE_WIDTH / 2 + 4, detalleY, normal, 9, INK);
        texto(pagina.contenido, "Documento: " + documentoCliente(comprobante), MARGIN, detalleY + 17, normal, 8, MUTED);
        texto(pagina.contenido, "Emisión: " + FECHA_FORMATO.format(comprobante.fechaEmision()), PAGE_WIDTH / 2 + 4, detalleY + 17, normal, 8, MUTED);
        if (comprobante.clienteEmail() != null && !comprobante.clienteEmail().isBlank()) {
            texto(pagina.contenido, "Correo: " + comprobante.clienteEmail(), MARGIN, detalleY + 34, normal, 8, MUTED);
        }
        pagina.y = detalleY + 58;
    }

    private float dibujarEncabezadoTabla(PdfPage pagina, PDFont normal, PDFont negrita, float top) throws IOException {
        float alto = 25;
        caja(pagina.contenido, MARGIN, top, CONTENT_WIDTH, alto, INK);
        texto(pagina.contenido, "NRO", MARGIN + 8, top + 8, negrita, 7, Color.WHITE);
        texto(pagina.contenido, "DESCRIPCION", MARGIN + 34, top + 8, negrita, 7, Color.WHITE);
        textoDerecha(pagina.contenido, "CANT.", 386, top + 8, negrita, 7, Color.WHITE);
        textoDerecha(pagina.contenido, "P. UNIT.", 458, top + 8, negrita, 7, Color.WHITE);
        textoDerecha(pagina.contenido, "IMPORTE", PAGE_WIDTH - MARGIN - 9, top + 8, negrita, 7, Color.WHITE);
        linea(pagina.contenido, MARGIN, top + alto, PAGE_WIDTH - MARGIN, top + alto, INK);
        return top + alto + 5;
    }

    private void dibujarFila(
        PdfPage pagina,
        int numero,
        DetallePedidoResponse detalle,
        List<String> nombre,
        PDFont normal,
        PDFont negrita,
        float alto
    ) throws IOException {
        float top = pagina.y;
        texto(pagina.contenido, String.valueOf(numero), MARGIN + 9, top + 9, normal, 8, INK);
        for (int indice = 0; indice < nombre.size(); indice++) {
            texto(
                pagina.contenido,
                nombre.get(indice),
                MARGIN + 34,
                top + 4 + indice * 12,
                indice == 0 ? negrita : normal,
                9,
                INK
            );
        }
        texto(pagina.contenido, valor(detalle.productoCodigo(), ""), MARGIN + 34, top + alto - 13, normal, 7, MUTED);
        textoDerecha(pagina.contenido, valor(detalle.cantidad(), 0).toString(), 386, top + 9, normal, 8, INK);
        textoDerecha(pagina.contenido, moneda(detalle.precioUnitario()), 458, top + 9, normal, 8, INK);
        textoDerecha(pagina.contenido, moneda(detalle.subtotal()), PAGE_WIDTH - MARGIN - 9, top + 9, negrita, 8, INK);
        linea(pagina.contenido, MARGIN, top + alto, PAGE_WIDTH - MARGIN, top + alto, RULE);
    }

    private void dibujarTotalesYEntrega(
        PdfPage pagina,
        PedidoComprobanteResponse comprobante,
        PDFont normal,
        PDFont negrita
    ) throws IOException {
        PedidoResponse pedido = comprobante.pedido();
        pagina.y += 18;
        texto(pagina.contenido, "SON " + importeEnLetras(pedido.total()), MARGIN, pagina.y, normal, 8, MUTED);
        float top = pagina.y + 22;
        float totalX = PAGE_WIDTH - MARGIN - 218;
        float totalW = 218;
        float totalH = 137;
        caja(pagina.contenido, totalX, top, totalW, totalH, PALE);
        float filaY = top + 12;
        importe(pagina.contenido, "Subtotal", pedido.subtotal(), totalX + 12, totalX + totalW - 12, filaY, normal, 8);
        filaY += 20;
        importe(pagina.contenido, "Descuento", pedido.descuentoCupon(), totalX + 12, totalX + totalW - 12, filaY, normal, 8);
        filaY += 20;
        importe(pagina.contenido, "IGV", pedido.igv(), totalX + 12, totalX + totalW - 12, filaY, normal, 8);
        filaY += 20;
        importe(pagina.contenido, "Envío", pedido.costoEnvio(), totalX + 12, totalX + totalW - 12, filaY, normal, 8);
        linea(pagina.contenido, totalX + 12, filaY + 17, totalX + totalW - 12, filaY + 17, RULE);
        importe(pagina.contenido, "TOTAL", pedido.total(), totalX + 12, totalX + totalW - 12, filaY + 27, negrita, 12);

        float infoY = top + 5;
        texto(pagina.contenido, "PAGO", MARGIN, infoY, negrita, 8, TERRACOTTA);
        infoY += 17;
        List<String> pagosAprobados = pedido.pagos().stream()
            .filter(pago -> "APROBADO".equals(pago.estado()))
            .map(pago -> pago.metodoPago() + " - " + pago.estado() + " - S/ " + moneda(pago.monto()))
            .toList();
        if (pagosAprobados.isEmpty()) {
            texto(pagina.contenido, "Pago aprobado", MARGIN, infoY, normal, 8, INK);
            infoY += 16;
        } else {
            for (String pago : pagosAprobados) {
                texto(pagina.contenido, pago, MARGIN, infoY, normal, 8, INK);
                infoY += 15;
            }
        }
        if (comprobante.sucursalNombre() != null && !comprobante.sucursalNombre().isBlank()) {
            texto(pagina.contenido, "Atendido por: " + comprobante.sucursalNombre(), MARGIN, infoY + 5, normal, 8, MUTED);
            infoY += 18;
        }
        if (pedido.datosEntrega() != null) {
            var entrega = pedido.datosEntrega();
            String destino = pedido.tipoEntrega() != null && pedido.tipoEntrega().equals("RECOJO_TIENDA")
                ? valor(entrega.sucursalNombre(), "Recojo en tienda")
                : valor(entrega.direccion(), "");
            if (!destino.isBlank()) {
                texto(pagina.contenido, "ENTREGA", MARGIN, infoY + 6, negrita, 8, TERRACOTTA);
                List<String> direccion = ajustarTexto(destino, normal, 8, totalX - MARGIN - 20);
                for (int indice = 0; indice < direccion.size(); indice++) {
                    texto(pagina.contenido, direccion.get(indice), MARGIN, infoY + 22 + indice * 11, normal, 8, MUTED);
                }
            }
        }
    }

    private void dibujarPie(
        PDDocument documento,
        PDPage pagina,
        PDFont normal,
        PDFont negrita,
        int paginas,
        int numeroPagina
    ) throws IOException {
        try (PDPageContentStream pie = new PDPageContentStream(
            documento,
            pagina,
            PDPageContentStream.AppendMode.APPEND,
            true
        )) {
            linea(pie, MARGIN, PAGE_HEIGHT - 48, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 48, RULE);
            texto(
                pie,
                "Documento informativo. No reemplaza un comprobante tributario electrónico emitido ante SUNAT.",
                MARGIN,
                PAGE_HEIGHT - 38,
                normal,
                7,
                MUTED
            );
            textoDerecha(pie, "Página " + numeroPagina + " de " + paginas, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 38, negrita, 7, MUTED);
        }
    }

    private void importe(
        PDPageContentStream contenido,
        String etiqueta,
        BigDecimal monto,
        float x,
        float derecha,
        float top,
        PDFont fuente,
        float tamano
    ) throws IOException {
        texto(contenido, etiqueta, x, top, fuente, tamano, INK);
        textoDerecha(contenido, "S/ " + moneda(monto), derecha, top, fuente, tamano, INK);
    }

    private List<String> ajustarTexto(String texto, PDFont fuente, float tamano, float anchoMaximo) throws IOException {
        List<String> resultado = new ArrayList<>();
        StringBuilder linea = new StringBuilder();
        for (String palabra : texto.split("\\s+")) {
            String candidato = linea.isEmpty() ? palabra : linea + " " + palabra;
            if (!linea.isEmpty() && fuente.getStringWidth(candidato) / 1000 * tamano > anchoMaximo) {
                resultado.add(linea.toString());
                linea.setLength(0);
            }
            linea.append(linea.isEmpty() ? palabra : " " + palabra);
        }
        if (!linea.isEmpty()) resultado.add(linea.toString());
        return resultado.isEmpty() ? List.of("") : resultado;
    }

    private void texto(
        PDPageContentStream contenido,
        String valor,
        float x,
        float top,
        PDFont fuente,
        float tamano,
        Color color
    ) throws IOException {
        contenido.beginText();
        contenido.setFont(fuente, tamano);
        contenido.setNonStrokingColor(color);
        contenido.newLineAtOffset(x, PAGE_HEIGHT - top - tamano);
        contenido.showText(valor);
        contenido.endText();
    }

    private void textoDerecha(
        PDPageContentStream contenido,
        String valor,
        float derecha,
        float top,
        PDFont fuente,
        float tamano,
        Color color
    ) throws IOException {
        float ancho = fuente.getStringWidth(valor) / 1000 * tamano;
        texto(contenido, valor, derecha - ancho, top, fuente, tamano, color);
    }

    private void caja(PDPageContentStream contenido, float x, float top, float ancho, float alto, Color color) throws IOException {
        contenido.setNonStrokingColor(color);
        contenido.addRect(x, PAGE_HEIGHT - top - alto, ancho, alto);
        contenido.fill();
    }

    private void linea(PDPageContentStream contenido, float x1, float top1, float x2, float top2, Color color) throws IOException {
        contenido.setStrokingColor(color);
        contenido.setLineWidth(0.7f);
        contenido.moveTo(x1, PAGE_HEIGHT - top1);
        contenido.lineTo(x2, PAGE_HEIGHT - top2);
        contenido.stroke();
    }

    private String documentoCliente(PedidoComprobanteResponse comprobante) {
        if (comprobante.ruc() != null && !comprobante.ruc().isBlank()) return "RUC " + comprobante.ruc();
        if (comprobante.clienteNumeroDocumento() == null || comprobante.clienteNumeroDocumento().isBlank()) {
            return "Consumidor final";
        }
        return valor(comprobante.clienteTipoDocumento(), "Documento") + " " + comprobante.clienteNumeroDocumento();
    }

    private String tipoComprobante(String tipo) {
        return "FACTURA".equalsIgnoreCase(tipo) ? "FACTURA" : "BOLETA DE VENTA";
    }

    private String valor(String preferido, String alternativo) {
        return preferido == null || preferido.isBlank() ? alternativo : preferido;
    }

    private String moneda(BigDecimal importe) {
        return importe == null ? "0.00" : importe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String importeEnLetras(BigDecimal importe) {
        if (importe == null) {
            return "CERO SOLES";
        }
        int entero = importe.setScale(0, RoundingMode.HALF_UP).intValue();
        int centimos = importe.setScale(2, RoundingMode.HALF_UP).remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).abs().intValue();
        return numerosEnLetras(entero) + " CON " + String.format("%02d/100", centimos) + " SOLES";
    }

    private String numerosEnLetras(int numero) {
        if (numero == 0) return "CERO";
        if (numero < 0) return "MENOS " + numerosEnLetras(Math.abs(numero));
        if (numero < 20) return new String[]{"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE", "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE"}[numero];
        if (numero < 100) {
            int decena = numero / 10;
            int resto = numero % 10;
            String decenas = new String[]{"", "", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"}[decena];
            if (decena == 2 && resto > 0) return "VEINTI" + numerosEnLetras(resto).toLowerCase().toUpperCase();
            return resto == 0 ? decenas : decenas + " Y " + numerosEnLetras(resto);
        }
        if (numero < 1000) {
            int centena = numero / 100;
            int resto = numero % 100;
            String centenas = new String[]{"", "CIEN", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"}[centena];
            if (resto == 0) return centenas;
            if (centena == 1) return "CIENTO " + numerosEnLetras(resto);
            return centenas + " " + numerosEnLetras(resto);
        }
        if (numero < 1000000) {
            int miles = numero / 1000;
            int resto = numero % 1000;
            String milesText = miles == 1 ? "MIL" : numerosEnLetras(miles) + " MIL";
            return resto == 0 ? milesText : milesText + " " + numerosEnLetras(resto);
        }
        int millones = numero / 1000000;
        int resto = numero % 1000000;
        String millonesText = millones == 1 ? "UN MILLON" : numerosEnLetras(millones) + " MILLONES";
        return resto == 0 ? millonesText : millonesText + " " + numerosEnLetras(resto);
    }

    private String valor(Integer preferido, Integer alternativo) {
        return preferido == null ? alternativo.toString() : preferido.toString();
    }

    private static final class PdfPage {
        private final PDPage pagina;
        private final PDPageContentStream contenido;
        private float y;

        private PdfPage(PDPage pagina, PDPageContentStream contenido) {
            this.pagina = pagina;
            this.contenido = contenido;
        }
    }
}
