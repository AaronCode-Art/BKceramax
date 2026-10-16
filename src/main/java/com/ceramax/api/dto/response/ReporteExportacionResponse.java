package com.ceramax.api.dto.response;

import com.ceramax.api.model.reportes.ReporteExportacion.EstadoReporte;
import com.ceramax.api.model.reportes.ReporteExportacion.FormatoReporte;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReporteExportacionResponse(
    UUID id,
    FormatoReporte formato,
    EstadoReporte estado,
    LocalDate desde,
    LocalDate hasta,
    OffsetDateTime creadoEn,
    OffsetDateTime iniciadoEn,
    OffsetDateTime completadoEn,
    OffsetDateTime expiraEn,
    String mensajeError,
    String urlEstado,
    String urlDescarga
) {}
