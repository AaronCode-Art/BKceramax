package com.ceramax.api.controller.resena;

import com.ceramax.api.dto.request.ActualizarResenaRequest;
import com.ceramax.api.dto.request.CrearResenaRequest;
import com.ceramax.api.dto.response.ResenaResponse;
import com.ceramax.api.dto.response.ResumenResenasResponse;
import com.ceramax.api.service.resena.ResenaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
public class ResenaController {

    private final ResenaService resenaService;

    public ResenaController(ResenaService resenaService) {
        this.resenaService = resenaService;
    }

    @PostMapping("/api/v1/cliente/resenas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ResenaResponse> crear(@Valid @RequestBody CrearResenaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resenaService.crear(request));
    }

    @PostMapping(value = "/api/v1/cliente/resenas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ResenaResponse> crearConImagenes(
        @Valid @RequestPart("resena") CrearResenaRequest request,
        @RequestPart(value = "imagenes", required = false) List<MultipartFile> imagenes
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resenaService.crearConImagenes(request, imagenes));
    }

    @PutMapping("/api/v1/cliente/resenas/{resenaId}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ResenaResponse> actualizar(
        @PathVariable UUID resenaId,
        @Valid @RequestBody ActualizarResenaRequest request
    ) {
        return ResponseEntity.ok(resenaService.actualizar(resenaId, request));
    }

    @PutMapping(
        value = "/api/v1/cliente/resenas/{resenaId}",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ResenaResponse> actualizarConImagenes(
        @PathVariable UUID resenaId,
        @Valid @RequestPart("resena") ActualizarResenaRequest request,
        @RequestPart(value = "imagenes", required = false) List<MultipartFile> imagenes
    ) {
        return ResponseEntity.ok(resenaService.actualizarConImagenes(resenaId, request, imagenes));
    }

    @DeleteMapping("/api/v1/cliente/resenas/{resenaId}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID resenaId) {
        resenaService.eliminar(resenaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/public/catalogo/{productoId}/resenas")
    public ResponseEntity<Page<ResenaResponse>> listarPublicas(
        @PathVariable UUID productoId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(resenaService.listarPublicas(productoId, pageable));
    }

    @GetMapping("/api/v1/public/catalogo/{productoId}/resenas/resumen")
    public ResponseEntity<ResumenResenasResponse> obtenerResumen(@PathVariable UUID productoId) {
        return ResponseEntity.ok(resenaService.obtenerResumen(productoId));
    }
}
