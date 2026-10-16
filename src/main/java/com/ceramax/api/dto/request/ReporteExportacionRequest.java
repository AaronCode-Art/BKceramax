package com.ceramax.api.dto.request;

import com.ceramax.api.model.reportes.ReporteExportacion.FormatoReporte;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ReporteExportacionRequest(
    @NotNull LocalDate desde,
    @NotNull LocalDate hasta,
    @NotNull FormatoReporte formato
) {}
