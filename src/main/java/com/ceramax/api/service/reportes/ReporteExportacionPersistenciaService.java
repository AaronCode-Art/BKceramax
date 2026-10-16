package com.ceramax.api.service.reportes;

import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.model.reportes.ReporteExportacion;
import com.ceramax.api.model.reportes.ReporteExportacion.EstadoReporte;
import com.ceramax.api.model.reportes.ReporteExportacion.FormatoReporte;
import com.ceramax.api.repository.reportes.ReporteExportacionRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteExportacionPersistenciaService {

    private final ReporteExportacionRepository repository;
    private final Duration retencion;

    public ReporteExportacionPersistenciaService(
        ReporteExportacionRepository repository,
        @Value("${reports.jobs.retention:24h}") Duration retencion
    ) {
        this.repository = repository;
        this.retencion = retencion;
        if (retencion.isZero() || retencion.isNegative()) {
            throw new IllegalArgumentException("reports.jobs.retention debe ser positivo");
        }
    }

    @Transactional
    public ReporteExportacion crear(
        String solicitante,
        FormatoReporte formato,
        java.time.LocalDate desde,
        java.time.LocalDate hasta
    ) {
        ReporteExportacion reporte = new ReporteExportacion();
        reporte.setId(UUID.randomUUID());
        reporte.setSolicitadoPor(solicitante);
        reporte.setFormato(formato);
        reporte.setEstado(EstadoReporte.PENDIENTE);
        reporte.setDesde(desde);
        reporte.setHasta(hasta);
        reporte.setCreadoEn(OffsetDateTime.now(ZoneOffset.UTC));
        reporte.setExpiraEn(OffsetDateTime.now(ZoneOffset.UTC).plus(retencion));
        return repository.saveAndFlush(reporte);
    }

    @Transactional(readOnly = true)
    public ReporteExportacion obtener(UUID id, String solicitante) {
        return repository.findByIdAndSolicitadoPor(id, solicitante)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró la exportación solicitada"));
    }

    @Transactional
    public Optional<ReporteExportacion> iniciar(UUID id) {
        ReporteExportacion reporte = repository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró la exportación solicitada"));
        if (reporte.getEstado() != EstadoReporte.PENDIENTE) {
            return Optional.empty();
        }
        reporte.iniciar();
        return Optional.of(repository.saveAndFlush(reporte));
    }

    @Transactional
    public void completar(UUID id, String nombreArchivo, String tipoContenido) {
        ReporteExportacion reporte = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró la exportación solicitada"));
        reporte.completar(
            nombreArchivo,
            tipoContenido,
            OffsetDateTime.now(ZoneOffset.UTC).plus(retencion)
        );
        repository.saveAndFlush(reporte);
    }

    @Transactional
    public void fallar(UUID id, String mensaje) {
        repository.findById(id).ifPresent(reporte -> {
            reporte.fallar(mensaje);
            repository.saveAndFlush(reporte);
        });
    }

    @Transactional
    public void expirar(UUID id) {
        repository.findById(id).ifPresent(reporte -> {
            reporte.expirar();
            repository.saveAndFlush(reporte);
        });
    }

    @Transactional(readOnly = true)
    public List<ReporteExportacion> buscar(EstadoReporte estado) {
        return repository.findByEstado(estado);
    }

    @Transactional
    public List<ReporteExportacion> buscarExpiradas(OffsetDateTime ahora) {
        return repository.findByEstadoAndExpiraEnBefore(EstadoReporte.COMPLETADO, ahora);
    }

    @Transactional
    public List<ReporteExportacion> buscarPendientesAntiguas(OffsetDateTime limite) {
        return repository.findByEstadoAndCreadoEnBefore(EstadoReporte.PENDIENTE, limite);
    }
}
