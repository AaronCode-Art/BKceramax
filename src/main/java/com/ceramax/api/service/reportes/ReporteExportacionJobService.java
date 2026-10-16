package com.ceramax.api.service.reportes;

import com.ceramax.api.dto.request.ReporteExportacionRequest;
import com.ceramax.api.dto.response.ReporteExportacionResponse;
import com.ceramax.api.exception.ExternalServiceUnavailableException;
import com.ceramax.api.model.reportes.ReporteExportacion;
import com.ceramax.api.model.reportes.ReporteExportacion.EstadoReporte;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Service
public class ReporteExportacionJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporteExportacionJobService.class);
    private static final String URL_BASE = "/api/v1/reportes/ventas-inventario/exportaciones/";

    private final ReporteExportacionPersistenciaService persistencia;
    private final ReporteExportacionStorageService storage;
    private final ReporteExportacionWorker worker;
    private final ReporteService reporteService;

    public ReporteExportacionJobService(
        ReporteExportacionPersistenciaService persistencia,
        ReporteExportacionStorageService storage,
        ReporteExportacionWorker worker,
        ReporteService reporteService
    ) {
        this.persistencia = persistencia;
        this.storage = storage;
        this.worker = worker;
        this.reporteService = reporteService;
    }

    public ReporteExportacionResponse crear(ReporteExportacionRequest solicitud, String solicitante) {
        reporteService.validarRangoFechas(solicitud.desde(), solicitud.hasta());
        ReporteExportacion reporte = persistencia.crear(
            solicitante,
            solicitud.formato(),
            solicitud.desde(),
            solicitud.hasta()
        );
        try {
            worker.generar(reporte.getId());
        } catch (TaskRejectedException exception) {
            persistencia.fallar(
                reporte.getId(),
                "El servicio está ocupado. Intente solicitar el reporte nuevamente."
            );
            throw new ExternalServiceUnavailableException(
                "No hay capacidad disponible para iniciar la exportación; solicítela nuevamente",
                exception
            );
        }
        return respuesta(reporte);
    }

    public ReporteExportacionResponse estado(UUID id, String solicitante) {
        ReporteExportacion reporte = persistencia.obtener(id, solicitante);
        if (estaExpirada(reporte)) {
            expirar(reporte);
            throw new ResponseStatusException(HttpStatus.GONE, "La exportación ha expirado");
        }
        return respuesta(reporte);
    }

    public ArchivoReporte descargar(UUID id, String solicitante) {
        ReporteExportacion reporte = persistencia.obtener(id, solicitante);
        if (estaExpirada(reporte)) {
            expirar(reporte);
            throw new ResponseStatusException(HttpStatus.GONE, "La exportación ha expirado");
        }
        if (reporte.getEstado() != EstadoReporte.COMPLETADO || reporte.getNombreArchivo() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La exportación todavía no está disponible");
        }
        try {
            return new ArchivoReporte(
                storage.leer(reporte.getNombreArchivo()),
                reporte.getContentType(),
                reporte.getNombreArchivo()
            );
        } catch (IOException exception) {
            LOGGER.error("No se encontró el archivo de la exportación {}", id, exception);
            expirar(reporte);
            throw new ResponseStatusException(HttpStatus.GONE, "El archivo de la exportación ya no está disponible");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperarPendientesAlIniciar() {
        try {
            storage.limpiarTemporales();
            for (ReporteExportacion reporte : persistencia.buscar(EstadoReporte.EN_PROCESO)) {
                eliminarArtefactoResidual(reporte);
                persistencia.fallar(
                    reporte.getId(),
                    "La aplicación se reinició durante la generación. Solicite una nueva exportación."
                );
            }
            for (ReporteExportacion reporte : persistencia.buscar(EstadoReporte.PENDIENTE)) {
                lanzar(reporte.getId());
            }
        } catch (DataAccessException exception) {
            LOGGER.warn(
                "No se pudieron recuperar los trabajos de reportes. Aplique " +
                    "scripts/reportes-exportacion-migration.sql: {}",
                exception.getMostSpecificCause().getMessage()
            );
        } catch (IOException exception) {
            LOGGER.error("No se pudieron limpiar archivos temporales de reportes al iniciar", exception);
        }
    }

    @Scheduled(fixedDelayString = "${reports.jobs.cleanup-interval-ms:3600000}")
    public void limpiarExpirados() {
        try {
            OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
            for (ReporteExportacion reporte : persistencia.buscarExpiradas(ahora)) {
                try {
                    storage.eliminar(reporte.getNombreArchivo());
                    persistencia.expirar(reporte.getId());
                } catch (IOException exception) {
                    LOGGER.error(
                        "No se pudo eliminar el archivo expirado de la exportación {}",
                        reporte.getId(),
                        exception
                    );
                }
            }
        } catch (DataAccessException exception) {
            LOGGER.warn(
                "No se pudo ejecutar la limpieza de exportaciones. Verifique PostgreSQL y la migración SQL: {}",
                exception.getMostSpecificCause().getMessage()
            );
        }
    }

    private void lanzar(UUID id) {
        try {
            worker.generar(id);
        } catch (TaskRejectedException exception) {
            persistencia.fallar(
                id,
                "La aplicación se reinició y no pudo volver a encolar el trabajo. Solicite una nueva exportación."
            );
            LOGGER.error("No se pudo reencolar la exportación {}", id, exception);
        }
    }

    private void eliminarArtefactoResidual(ReporteExportacion reporte) {
        try {
            storage.eliminar(reporte.getId(), reporte.getFormato());
        } catch (IOException exception) {
            LOGGER.error("No se pudo limpiar el archivo residual de la exportación {}", reporte.getId(), exception);
        }
    }

    private void expirar(ReporteExportacion reporte) {
        if (reporte.getNombreArchivo() != null) {
            try {
                storage.eliminar(reporte.getNombreArchivo());
            } catch (IOException exception) {
                LOGGER.error("No se pudo eliminar el archivo de la exportación {}", reporte.getId(), exception);
                return;
            }
        }
        persistencia.expirar(reporte.getId());
    }

    private boolean estaExpirada(ReporteExportacion reporte) {
        return reporte.getEstado() == EstadoReporte.EXPIRADO ||
            reporte.getExpiraEn().isBefore(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private ReporteExportacionResponse respuesta(ReporteExportacion reporte) {
        String base = URL_BASE + reporte.getId();
        String urlDescarga = reporte.getEstado() == EstadoReporte.COMPLETADO ? base + "/archivo" : null;
        return new ReporteExportacionResponse(
            reporte.getId(),
            reporte.getFormato(),
            reporte.getEstado(),
            reporte.getDesde(),
            reporte.getHasta(),
            reporte.getCreadoEn(),
            reporte.getIniciadoEn(),
            reporte.getCompletadoEn(),
            reporte.getExpiraEn(),
            reporte.getMensajeError(),
            base,
            urlDescarga
        );
    }

    public record ArchivoReporte(byte[] contenido, String tipoContenido, String nombreArchivo) {}
}
