package com.ceramax.api.controller.catalogo;

import com.ceramax.api.dto.response.CatalogoPublicoResponse;
import com.ceramax.api.dto.response.GaleriaImagenResponse;
import com.ceramax.api.dto.response.ProductoDetallePublicoResponse;
import com.ceramax.api.service.catalogo.CatalogoPublicoService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/catalogo")
public class CatalogoPublicoController {

    private final CatalogoPublicoService catalogoPublicoService;

    public CatalogoPublicoController(CatalogoPublicoService catalogoPublicoService) {
        this.catalogoPublicoService = catalogoPublicoService;
    }

    @GetMapping
    public ResponseEntity<List<CatalogoPublicoResponse>> listar() {
        return ResponseEntity.ok(catalogoPublicoService.listar());
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
