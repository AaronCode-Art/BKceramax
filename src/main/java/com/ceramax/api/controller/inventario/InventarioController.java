package com.ceramax.api.controller.inventario;

import com.ceramax.api.dto.request.AjusteInventarioRequest;
import com.ceramax.api.dto.request.CierreTrasladoRequest;
import com.ceramax.api.dto.request.EntradaInventarioRequest;
import com.ceramax.api.dto.request.TrasladoInventarioRequest;
import com.ceramax.api.dto.response.AjusteCreadoResponse;
import com.ceramax.api.dto.response.AjusteInventarioResponse;
import com.ceramax.api.dto.response.InventarioResponse;
import com.ceramax.api.dto.response.MovimientoInventarioResponse;
import com.ceramax.api.dto.response.OperacionInventarioResponse;
import com.ceramax.api.dto.response.ReservaInventarioResponse;
import com.ceramax.api.dto.response.StockDisponibleResponse;
import com.ceramax.api.dto.response.TrasladoCreadoResponse;
import com.ceramax.api.dto.response.TrasladoInventarioResponse;
import com.ceramax.api.service.inventario.InventarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<List<InventarioResponse>> listar() {
        return ResponseEntity.ok(inventarioService.listarInventario());
    }

    @GetMapping("/stock-disponible")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<List<StockDisponibleResponse>> listarStockDisponible() {
        return ResponseEntity.ok(inventarioService.listarStockDisponible());
    }

    @GetMapping("/almacenes/{almacenId}/productos/{productoId}/stock-disponible")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<Integer> consultarStockDisponible(
        @PathVariable UUID almacenId,
        @PathVariable UUID productoId
    ) {
        return ResponseEntity.ok(inventarioService.consultarStockDisponible(almacenId, productoId));
    }

    @GetMapping("/movimientos")
    public ResponseEntity<List<MovimientoInventarioResponse>> listarMovimientos() {
        return ResponseEntity.ok(inventarioService.listarMovimientos());
    }

    @GetMapping("/traslados/en-transito")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<List<TrasladoInventarioResponse>> listarTrasladosEnTransito() {
        return ResponseEntity.ok(inventarioService.listarTrasladosEnTransito());
    }

    @GetMapping("/reservas-activas")
    public ResponseEntity<List<ReservaInventarioResponse>> listarReservasActivas() {
        return ResponseEntity.ok(inventarioService.listarReservasActivas());
    }

    @GetMapping("/ajustes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AjusteInventarioResponse>> listarAjustes() {
        return ResponseEntity.ok(inventarioService.listarAjustes());
    }

    @PostMapping("/entradas")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<OperacionInventarioResponse> registrarEntrada(
        @Valid @RequestBody EntradaInventarioRequest request
    ) {
        return ResponseEntity.ok(inventarioService.registrarEntrada(request));
    }

    @GetMapping("/solicitudes-entradas")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<List<com.ceramax.api.dto.response.SolicitudEntradaResponse>> listarSolicitudesEntrada() {
        return ResponseEntity.ok(inventarioService.listarSolicitudesEntrada());
    }

    @PostMapping("/solicitudes-entradas/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA')")
    public ResponseEntity<OperacionInventarioResponse> aprobarSolicitudEntrada(@PathVariable UUID id) {
        return ResponseEntity.ok(inventarioService.aprobarSolicitudEntrada(id));
    }

    @PostMapping("/solicitudes-entradas/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA')")
    public ResponseEntity<OperacionInventarioResponse> rechazarSolicitudEntrada(@PathVariable UUID id, @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(inventarioService.rechazarSolicitudEntrada(id, motivo == null || motivo.isBlank() ? "Rechazada por el usuario" : motivo));
    }

    @PostMapping("/ajustes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AjusteCreadoResponse> registrarAjuste(
        @Valid @RequestBody AjusteInventarioRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.registrarAjuste(request));
    }

    @PostMapping("/traslados")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<TrasladoCreadoResponse> iniciarTraslado(
        @Valid @RequestBody TrasladoInventarioRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventarioService.iniciarTraslado(request));
    }

    @PostMapping("/traslados/{id}/recepcion")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<OperacionInventarioResponse> confirmarRecepcion(
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) CierreTrasladoRequest request
    ) {
        String observacion = request == null ? null : request.observacion();
        return ResponseEntity.ok(inventarioService.confirmarRecepcion(id, observacion));
    }

    @PostMapping("/traslados/{id}/cancelacion")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<OperacionInventarioResponse> cancelarTraslado(
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) CierreTrasladoRequest request
    ) {
        String motivo = request == null ? null : request.observacion();
        return ResponseEntity.ok(inventarioService.cancelarTraslado(id, motivo));
    }
}
