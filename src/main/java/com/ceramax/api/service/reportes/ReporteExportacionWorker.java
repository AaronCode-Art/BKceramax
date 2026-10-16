package com.ceramax.api.service.reportes;

import com.ceramax.api.model.reportes.ReporteExportacion;
import com.ceramax.api.model.reportes.ReporteExportacion.FormatoReporte;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ReporteExportacionWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporteExportacionWorker.class);
    private static final String TIPO_PDF = "application/pdf";
    private static final String TIPO_EXCEL =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReporteExportacionPersistenciaService persistencia;
    private final ReporteExportacionStorageService storage;
    private final ReporteService reporteService;

    public ReporteExportacionWorker(
        ReporteExportacionPersistenciaService persistencia,
        ReporteExportacionStorageService storage,
        ReporteService reporteService
    ) {
        this.persistencia = persistencia;
        this.storage = storage;
        this.reporteService = reporteService;
    }

    @Async("reportExecutor")
    public void generar(UUID id) {
        ReporteExportacion reporte = persistencia.iniciar(id).orElse(null);
        if (reporte == null) {
            return;
        }

        String nombreArchivo = null;
        try {
            byte[] contenido = reporte.getFormato() == FormatoReporte.PDF
                ? reporteService.exportarPdf(reporte.getDesde(), reporte.getHasta())
                : reporteService.exportarExcel(reporte.getDesde(), reporte.getHasta());
            nombreArchivo = storage.guardar(id, reporte.getFormato(), contenido);
            persistencia.completar(id, nombreArchivo, tipoContenido(reporte.getFormato()));
        } catch (IOException | RuntimeException exception) {
            if (nombreArchivo != null) {
                try {
                    storage.eliminar(nombreArchivo);
                } catch (IOException cleanupException) {
                    LOGGER.error("No se pudo retirar el archivo de una exportación fallida {}", id, cleanupException);
                }
            }
            LOGGER.error("Falló la generación de la exportación {}", id, exception);
            persistencia.fallar(id, "No se pudo generar el archivo. Solicite una nueva exportación.");
        }
    }

    private String tipoContenido(FormatoReporte formato) {
        return formato == FormatoReporte.PDF ? TIPO_PDF : TIPO_EXCEL;
    }
}
