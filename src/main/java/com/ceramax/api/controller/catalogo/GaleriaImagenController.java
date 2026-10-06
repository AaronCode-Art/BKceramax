package com.ceramax.api.controller.catalogo;

import com.ceramax.api.dto.request.GaleriaImagenRequest;
import com.ceramax.api.dto.response.GaleriaImagenResponse;
import com.ceramax.api.service.catalogo.GaleriaImagenService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA')")
@RequestMapping("/api/v1")
public class GaleriaImagenController {

    private final GaleriaImagenService galeriaImagenService;

    public GaleriaImagenController(GaleriaImagenService galeriaImagenService) {
        this.galeriaImagenService = galeriaImagenService;
    }

    @GetMapping("/catalogo/productos/{productoId}/galeria")
    public ResponseEntity<List<GaleriaImagenResponse>> listarPorProducto(@PathVariable UUID productoId) {
        return ResponseEntity.ok(galeriaImagenService.listarPorProducto(productoId));
    }

    @PostMapping("/catalogo/productos/{productoId}/galeria")
    public ResponseEntity<GaleriaImagenResponse> crear(@PathVariable UUID productoId, @Valid @RequestBody GaleriaImagenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(galeriaImagenService.crear(productoId, request));
    }

    @PostMapping(
        value = "/catalogo/productos/{productoId}/galeria/archivo",
        consumes = "multipart/form-data"
    )
    public ResponseEntity<GaleriaImagenResponse> cargarArchivo(
        @PathVariable UUID productoId,
        @RequestParam("archivo") MultipartFile archivo,
        @RequestParam(required = false) Short orden,
        @RequestParam(defaultValue = "false") Boolean esPrincipal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(galeriaImagenService.cargarArchivo(productoId, archivo, orden, esPrincipal));
    }

    @PutMapping("/catalogo/productos/{productoId}/galeria/{imagenId}")
    public ResponseEntity<GaleriaImagenResponse> actualizar(
        @PathVariable UUID productoId,
        @PathVariable UUID imagenId,
        @Valid @RequestBody GaleriaImagenRequest request
    ) {
        return ResponseEntity.ok(galeriaImagenService.actualizar(productoId, imagenId, request));
    }

    @PutMapping(
        value = "/catalogo/productos/{productoId}/galeria/{imagenId}/archivo",
        consumes = "multipart/form-data"
    )
    public ResponseEntity<GaleriaImagenResponse> reemplazarArchivo(
        @PathVariable UUID productoId,
        @PathVariable UUID imagenId,
        @RequestParam("archivo") MultipartFile archivo,
        @RequestParam(required = false) Short orden,
        @RequestParam(required = false) Boolean esPrincipal
    ) {
        return ResponseEntity.ok(
            galeriaImagenService.reemplazarArchivo(productoId, imagenId, archivo, orden, esPrincipal)
        );
    }

    @DeleteMapping("/catalogo/productos/{productoId}/galeria/{imagenId}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID productoId, @PathVariable UUID imagenId) {
        galeriaImagenService.eliminar(productoId, imagenId);
        return ResponseEntity.noContent().build();
    }

}
