package com.ceramax.api.service.reportes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.dto.request.ReporteExportacionRequest;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.model.reportes.ReporteExportacion;
import com.ceramax.api.model.reportes.ReporteExportacion.EstadoReporte;
import com.ceramax.api.model.reportes.ReporteExportacion.FormatoReporte;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReporteExportacionJobServiceTest {

    private final ReporteExportacionPersistenciaService persistencia =
        mock(ReporteExportacionPersistenciaService.class);
    private final ReporteExportacionStorageService storage = mock(ReporteExportacionStorageService.class);
    private final ReporteExportacionWorker worker = mock(ReporteExportacionWorker.class);
    private final ReporteService reporteService = mock(ReporteService.class);
    private final ReporteExportacionJobService service =
        new ReporteExportacionJobService(persistencia, storage, worker, reporteService);

    @Test
    void creaTrabajoPendienteYDevuelveRutasDeSeguimiento() {
        UUID id = UUID.randomUUID();
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        ReporteExportacionRequest solicitud = new ReporteExportacionRequest(desde, hasta, FormatoReporte.EXCEL);
        when(persistencia.crear("admin@example.com", FormatoReporte.EXCEL, desde, hasta))
            .thenReturn(reporte(id, desde, hasta));

        var resultado = service.crear(solicitud, "admin@example.com");

        assertEquals(id, resultado.id());
        assertEquals(EstadoReporte.PENDIENTE, resultado.estado());
        assertEquals(
            "/api/v1/reportes/ventas-inventario/exportaciones/" + id,
            resultado.urlEstado()
        );
        assertNull(resultado.urlDescarga());
        verify(worker).generar(id);
    }

    @Test
    void rechazaFechasInvertidasAntesDeCrearElTrabajo() {
        LocalDate desde = LocalDate.of(2026, 10, 31);
        LocalDate hasta = LocalDate.of(2026, 10, 1);

        org.mockito.Mockito.doThrow(new BusinessException("Rango inválido"))
            .when(reporteService)
            .validarRangoFechas(desde, hasta);

        assertThrows(
            BusinessException.class,
            () -> service.crear(
                new ReporteExportacionRequest(desde, hasta, FormatoReporte.PDF),
                "admin@example.com"
            )
        );

        verify(persistencia, never()).crear(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
        verify(worker, never()).generar(org.mockito.ArgumentMatchers.any());
    }

    private ReporteExportacion reporte(UUID id, LocalDate desde, LocalDate hasta) {
        ReporteExportacion reporte = new ReporteExportacion();
        reporte.setId(id);
        reporte.setSolicitadoPor("admin@example.com");
        reporte.setFormato(FormatoReporte.EXCEL);
        reporte.setEstado(EstadoReporte.PENDIENTE);
        reporte.setDesde(desde);
        reporte.setHasta(hasta);
        reporte.setCreadoEn(OffsetDateTime.now(ZoneOffset.UTC));
        reporte.setExpiraEn(OffsetDateTime.now(ZoneOffset.UTC).plusHours(24));
        return reporte;
    }
}
