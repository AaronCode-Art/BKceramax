package com.ceramax.api.controller.venta;

import com.ceramax.api.dto.request.CuponRequest;
import com.ceramax.api.dto.response.CuponAnuncioResponse;
import com.ceramax.api.dto.response.CuponResponse;
import com.ceramax.api.service.venta.CuponService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CuponController {

    private final CuponService cuponService;

    public CuponController(CuponService cuponService) {
        this.cuponService = cuponService;
    }

    @GetMapping("/public/cupones/anuncios")
    public ResponseEntity<List<CuponAnuncioResponse>> listarAnunciosVigentes() {
        return ResponseEntity.ok(cuponService.listarAnunciosVigentes());
    }

    @GetMapping("/cupones")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA')")
    public ResponseEntity<List<CuponResponse>> listar() {
        return ResponseEntity.ok(cuponService.listar());
    }

    @PostMapping("/cupones")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA')")
    public ResponseEntity<CuponResponse> crear(@Valid @RequestBody CuponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuponService.crear(request));
    }

    @PutMapping("/cupones/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA')")
    public ResponseEntity<CuponResponse> actualizar(
        @PathVariable UUID id,
        @Valid @RequestBody CuponRequest request
    ) {
        return ResponseEntity.ok(cuponService.actualizar(id, request));
    }

    @DeleteMapping("/cupones/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA')")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        cuponService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
