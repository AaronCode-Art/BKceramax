package com.ceramax.api.service.reportes;

import com.ceramax.api.dto.response.ReporteVentasInventarioResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.InventarioReporteResponse;
import com.ceramax.api.dto.response.ReporteVentasInventarioResponse.ProductoMasVendidoResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.repository.reportes.ReporteRepository;
import com.ceramax.api.repository.reportes.ReporteRepository.InventarioActual;
import com.ceramax.api.repository.reportes.ReporteRepository.ProductoMasVendido;
import com.ceramax.api.repository.reportes.ReporteRepository.ResumenVentas;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final ReporteExportService exportService;

    public ReporteService(ReporteRepository reporteRepository, ReporteExportService exportService) {
        this.reporteRepository = reporteRepository;
        this.exportService = exportService;
    }

    @Transactional(readOnly = true)
    public ReporteVentasInventarioResponse generar(LocalDate desde, LocalDate hasta) {
        return generar(desde, hasta, true);
    }

    @Transactional(readOnly = true)
    public byte[] exportarExcel(LocalDate desde, LocalDate hasta) {
        ReporteVentasInventarioResponse reporte = generar(desde, hasta, false);
        Consumer<Consumer<InventarioReporteResponse>> inventario = escritor ->
            reporteRepository.recorrerInventarioActual(item -> escritor.accept(mapear(item)));
        return exportService.generarExcel(reporte, inventario);
    }

    private ReporteVentasInventarioResponse generar(LocalDate desde, LocalDate hasta, boolean incluirInventario) {
        validarRango(desde, hasta);
        OffsetDateTime inicioUtc = desde.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime finUtc = hasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        ResumenVentas resumen = reporteRepository.obtenerResumenVentas(inicioUtc, finUtc);
        List<ProductoMasVendidoResponse> productos = reporteRepository
            .listarProductosMasVendidos(inicioUtc, finUtc)
            .stream()
            .map(this::mapear)
            .toList();
        List<InventarioReporteResponse> inventario = incluirInventario
            ? reporteRepository.listarInventarioActual().stream().map(this::mapear).toList()
            : List.of();

        return new ReporteVentasInventarioResponse(
            desde,
            hasta,
            OffsetDateTime.now(ZoneOffset.UTC),
            resumen.cantidadVentas(),
            resumen.ventasWeb(),
            resumen.ventasPresenciales(),
            resumen.ingresosTotales(),
            productos,
            inventario
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportarPdf(LocalDate desde, LocalDate hasta) {
        return exportService.generarPdf(generar(desde, hasta));
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new BusinessException("Las fechas desde y hasta son obligatorias");
        }
        if (hasta.isBefore(desde)) {
            throw new BusinessException("La fecha hasta no puede ser anterior a desde");
        }
        if (hasta.equals(LocalDate.MAX)) {
            throw new BusinessException("La fecha hasta está fuera del rango permitido");
        }
    }

    private ProductoMasVendidoResponse mapear(ProductoMasVendido producto) {
        return new ProductoMasVendidoResponse(
            producto.productoId(),
            producto.productoCodigo(),
            producto.productoNombre(),
            producto.unidadesVendidas(),
            producto.importeVendido()
        );
    }

    private InventarioReporteResponse mapear(InventarioActual item) {
        return new InventarioReporteResponse(
            item.almacenId(),
            item.almacenCodigo(),
            item.almacenNombre(),
            item.productoId(),
            item.productoCodigo(),
            item.productoNombre(),
            item.categoriaNombre(),
            item.stockFisico(),
            item.stockReservado(),
            item.stockDisponible(),
            item.stockMinimo(),
            item.stockBajo()
        );
    }
}
