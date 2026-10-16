package com.ceramax.api.model.reportes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reporte_exportacion")
@Getter
@Setter
@NoArgsConstructor
public class ReporteExportacion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "solicitado_por", nullable = false, length = 120)
    private String solicitadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "formato", nullable = false, length = 8)
    private FormatoReporte formato;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoReporte estado;

    @Column(name = "desde", nullable = false)
    private java.time.LocalDate desde;

    @Column(name = "hasta", nullable = false)
    private java.time.LocalDate hasta;

    @Column(name = "nombre_archivo", length = 120)
    private String nombreArchivo;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "iniciado_en")
    private OffsetDateTime iniciadoEn;

    @Column(name = "completado_en")
    private OffsetDateTime completadoEn;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    public enum FormatoReporte {
        PDF,
        EXCEL
    }

    public enum EstadoReporte {
        PENDIENTE,
        EN_PROCESO,
        COMPLETADO,
        FALLIDO,
        EXPIRADO
    }

    public void iniciar() {
        estado = EstadoReporte.EN_PROCESO;
        iniciadoEn = OffsetDateTime.now(ZoneOffset.UTC);
        mensajeError = null;
    }

    public void completar(String fileName, String mimeType, OffsetDateTime expiresAt) {
        estado = EstadoReporte.COMPLETADO;
        nombreArchivo = fileName;
        contentType = mimeType;
        completadoEn = OffsetDateTime.now(ZoneOffset.UTC);
        expiraEn = expiresAt;
        mensajeError = null;
    }

    public void fallar(String message) {
        estado = EstadoReporte.FALLIDO;
        completadoEn = OffsetDateTime.now(ZoneOffset.UTC);
        mensajeError = message;
        nombreArchivo = null;
        contentType = null;
    }

    public void expirar() {
        estado = EstadoReporte.EXPIRADO;
        nombreArchivo = null;
        contentType = null;
    }
}
