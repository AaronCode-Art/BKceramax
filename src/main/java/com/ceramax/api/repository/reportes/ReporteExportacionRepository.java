package com.ceramax.api.repository.reportes;

import com.ceramax.api.model.reportes.ReporteExportacion;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReporteExportacionRepository extends JpaRepository<ReporteExportacion, UUID> {

    Optional<ReporteExportacion> findByIdAndSolicitadoPor(UUID id, String solicitadoPor);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReporteExportacion r where r.id = :id")
    Optional<ReporteExportacion> findByIdForUpdate(@Param("id") UUID id);

    List<ReporteExportacion> findByEstadoAndExpiraEnBefore(
        ReporteExportacion.EstadoReporte estado,
        OffsetDateTime fecha
    );

    List<ReporteExportacion> findByEstado(ReporteExportacion.EstadoReporte estado);

    List<ReporteExportacion> findByEstadoAndCreadoEnBefore(
        ReporteExportacion.EstadoReporte estado,
        OffsetDateTime fecha
    );
}
