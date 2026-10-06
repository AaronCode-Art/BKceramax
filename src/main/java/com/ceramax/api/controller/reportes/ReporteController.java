package com.ceramax.api.controller.reportes;

import com.ceramax.api.dto.response.ReporteVentasInventarioResponse;
import com.ceramax.api.service.reportes.ReporteService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reportes/ventas-inventario")
@PreAuthorize("hasRole('ADMIN')")
public class ReporteController {

    private static final String TIPO_PDF = "application/pdf";
    private static final String TIPO_EXCEL =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    public ResponseEntity<ReporteVentasInventarioResponse> generar(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return ResponseEntity.ok(reporteService.generar(desde, hasta));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportarPdf(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return archivo(reporteService.exportarPdf(desde, hasta), TIPO_PDF, "pdf", desde, hasta);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportarExcel(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return archivo(reporteService.exportarExcel(desde, hasta), TIPO_EXCEL, "xlsx", desde, hasta);
    }

    private ResponseEntity<byte[]> archivo(
        byte[] contenido,
        String tipo,
        String extension,
        LocalDate desde,
        LocalDate hasta
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(tipo));
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("ceramax-ventas-inventario-" + desde + "-" + hasta + "." + extension)
            .build());
        headers.setContentLength(contenido.length);
        return new ResponseEntity<>(contenido, headers, HttpStatus.OK);
    }
}
