package com.ceramax.api.controller.venta;

import com.ceramax.api.dto.request.ActualizarCarritoItemRequest;
import com.ceramax.api.dto.request.CarritoItemRequest;
import com.ceramax.api.dto.response.CarritoItemResponse;
import com.ceramax.api.service.venta.CarritoService;
import com.ceramax.api.service.venta.ListaDeseosService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@PreAuthorize("hasRole('CLIENTE')")
@RequestMapping("/api/v1/cliente")
public class TiendaClienteController {

    private final CarritoService carritoService;
    private final ListaDeseosService listaDeseosService;

    public TiendaClienteController(CarritoService carritoService, ListaDeseosService listaDeseosService) {
        this.carritoService = carritoService;
        this.listaDeseosService = listaDeseosService;
    }

    @GetMapping("/carrito")
    public ResponseEntity<List<CarritoItemResponse>> obtenerCarrito() {
        return ResponseEntity.ok(carritoService.obtener());
    }

    @PostMapping("/carrito/items")
    public ResponseEntity<List<CarritoItemResponse>> agregarAlCarrito(
        @Valid @RequestBody CarritoItemRequest request
    ) {
        return ResponseEntity.ok(carritoService.agregar(request));
    }

    @PutMapping("/carrito/items/{productoId}")
    public ResponseEntity<List<CarritoItemResponse>> actualizarCantidad(
        @PathVariable UUID productoId,
        @Valid @RequestBody ActualizarCarritoItemRequest request
    ) {
        return ResponseEntity.ok(carritoService.actualizar(productoId, request));
    }

    @DeleteMapping("/carrito/items/{productoId}")
    public ResponseEntity<List<CarritoItemResponse>> eliminarDelCarrito(@PathVariable UUID productoId) {
        return ResponseEntity.ok(carritoService.eliminar(productoId));
    }

    @DeleteMapping("/carrito")
    public ResponseEntity<Void> vaciarCarrito() {
        carritoService.vaciar();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/deseos")
    public ResponseEntity<List<UUID>> obtenerListaDeseos() {
        return ResponseEntity.ok(listaDeseosService.obtener());
    }

    @PostMapping("/deseos/{productoId}")
    public ResponseEntity<List<UUID>> agregarADeseos(@PathVariable UUID productoId) {
        return ResponseEntity.ok(listaDeseosService.agregar(productoId));
    }

    @DeleteMapping("/deseos/{productoId}")
    public ResponseEntity<List<UUID>> eliminarDeDeseos(@PathVariable UUID productoId) {
        return ResponseEntity.ok(listaDeseosService.eliminar(productoId));
    }
}
