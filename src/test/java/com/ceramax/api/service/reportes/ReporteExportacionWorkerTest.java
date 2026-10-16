package com.ceramax.api.service.reportes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.model.reportes.ReporteExportacion;
import com.ceramax.api.model.reportes.ReporteExportacion.EstadoReporte;
import com.ceramax.api.model.reportes.ReporteExportacion.FormatoReporte;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReporteExportacionWorkerTest {

    private final ReporteExportacionPersistenciaService persistencia =
        mock(ReporteExportacionPersistenciaService.class);
    private final ReporteExportacionStorageService storage = mock(ReporteExportacionStorageService.class);
    private final ReporteService reporteService = mock(ReporteService.class);
    private final ReporteExportacionWorker worker =
        new ReporteExportacionWorker(persistencia, storage, reporteService);

    @Test
    void guardaElArchivoAntesDeMarcarElTrabajoCompletado() throws Exception {
        UUID id = UUID.randomUUID();
        when(persistencia.iniciar(id)).thenReturn(Optional.of(reporte(id, FormatoReporte.PDF)));
        when(reporteService.exportarPdf(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2)
        )).thenReturn(new byte[] {1, 2, 3});
        when(storage.guardar(id, FormatoReporte.PDF, new byte[] {1, 2, 3})).thenReturn(id + ".pdf");

        worker.generar(id);

        verify(persistencia).completar(id, id + ".pdf", "application/pdf");
        verify(persistencia, never()).fallar(org.mockito.ArgumentMatchers.eq(id), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registraElErrorYNoMarcaComoCompletadoSiFallaLaGeneracion() {
        UUID id = UUID.randomUUID();
        when(persistencia.iniciar(id)).thenReturn(Optional.of(reporte(id, FormatoReporte.EXCEL)));
        when(reporteService.exportarExcel(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2)
        )).thenThrow(new IllegalStateException("fallo interno"));

        worker.generar(id);

        verify(persistencia).fallar(
            id,
            "No se pudo generar el archivo. Solicite una nueva exportación."
        );
        verify(persistencia, never()).completar(
            org.mockito.ArgumentMatchers.eq(id),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void noProcesaUnTrabajoQueYaNoEstaPendiente() {
        UUID id = UUID.randomUUID();
        when(persistencia.iniciar(id)).thenReturn(Optional.empty());

        worker.generar(id);

        verify(reporteService, never()).exportarExcel(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    private ReporteExportacion reporte(UUID id, FormatoReporte formato) {
        ReporteExportacion reporte = new ReporteExportacion();
        reporte.setId(id);
        reporte.setFormato(formato);
        reporte.setEstado(EstadoReporte.EN_PROCESO);
        reporte.setDesde(LocalDate.of(2026, 10, 1));
        reporte.setHasta(LocalDate.of(2026, 10, 2));
        return reporte;
    }
}
