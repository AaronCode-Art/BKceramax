package com.ceramax.api.service.reportes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.repository.reportes.ReporteRepository;
import com.ceramax.api.repository.reportes.ReporteRepository.ResumenVentas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReporteServiceTest {

    private final ReporteRepository reporteRepository = org.mockito.Mockito.mock(ReporteRepository.class);
    private final ReporteExportService exportService = org.mockito.Mockito.mock(ReporteExportService.class);
    private final ReporteService service = new ReporteService(reporteRepository, exportService);

    @Test
    void generaReporteConRangoUtcInclusivoYInventarioActual() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 3);
        OffsetDateTime inicio = desde.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = hasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        when(reporteRepository.obtenerResumenVentas(inicio, fin))
            .thenReturn(new ResumenVentas(0L, 0L, 0L, BigDecimal.ZERO));
        when(reporteRepository.listarProductosMasVendidos(inicio, fin)).thenReturn(List.of());
        when(reporteRepository.listarInventarioActual()).thenReturn(List.of());

        var reporte = service.generar(desde, hasta);

        assertEquals(desde, reporte.desde());
        assertEquals(hasta, reporte.hasta());
        assertEquals(BigDecimal.ZERO, reporte.ingresosTotales());
        assertEquals(List.of(), reporte.productosMasVendidos());
        assertEquals(ZoneOffset.UTC, reporte.generadoEn().getOffset());
        verify(reporteRepository).obtenerResumenVentas(inicio, fin);
    }

    @Test
    void rechazaRangoDeFechasInvertidoAntesDeConsultar() {
        assertThrows(
            BusinessException.class,
            () -> service.generar(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1))
        );

        verify(reporteRepository, never()).obtenerResumenVentas(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }
}
