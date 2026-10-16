package com.ceramax.api.controller.catalogo;

import com.ceramax.api.dto.response.CatalogoPublicoResponse;
import com.ceramax.api.dto.response.GaleriaImagenResponse;
import com.ceramax.api.dto.response.ProductoDetallePublicoResponse;
import com.ceramax.api.service.catalogo.CatalogoPublicoService;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/public/catalogo")
public class CatalogoPublicoController {

    private final CatalogoPublicoService catalogoPublicoService;

    public CatalogoPublicoController(CatalogoPublicoService catalogoPublicoService) {
        this.catalogoPublicoService = catalogoPublicoService;
    }

    @GetMapping
    public ResponseEntity<Page<CatalogoPublicoResponse>> listar(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(catalogoPublicoService.listar(PageRequest.of(page, size)));
    }

    @GetMapping("/{productoId}")
    public ResponseEntity<CatalogoPublicoResponse> obtener(@PathVariable UUID productoId) {
        return ResponseEntity.ok(catalogoPublicoService.obtener(productoId));
    }

    @GetMapping("/{productoId}/detalle")
    public ResponseEntity<ProductoDetallePublicoResponse> obtenerDetalle(@PathVariable UUID productoId) {
        return ResponseEntity.ok(catalogoPublicoService.obtenerDetalle(productoId));
    }

    @GetMapping("/{productoId}/galeria")
    public ResponseEntity<List<GaleriaImagenResponse>> listarGaleria(@PathVariable UUID productoId) {
        return ResponseEntity.ok(catalogoPublicoService.listarGaleria(productoId));
    }
}
