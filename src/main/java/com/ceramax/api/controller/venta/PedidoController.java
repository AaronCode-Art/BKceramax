package com.ceramax.api.controller.venta;

import com.ceramax.api.dto.request.AsignarDeliveryRequest;
import com.ceramax.api.dto.request.CambiarEstadoPedidoRequest;
import com.ceramax.api.dto.request.CotizarCheckoutWebRequest;
import com.ceramax.api.dto.request.CrearPedidoWebRequest;
import com.ceramax.api.dto.response.CheckoutCotizacionResponse;
import com.ceramax.api.dto.request.VentaPresencialRequest;
import com.ceramax.api.dto.response.PedidoPaginaResponse;
import com.ceramax.api.dto.response.PedidoResponse;
import com.ceramax.api.dto.response.PedidoEstadoHistorialResponse;
import com.ceramax.api.dto.response.DeliveryOpcionResponse;
import com.ceramax.api.dto.response.VentaPresencialResponse;
import com.ceramax.api.service.venta.ComprobantePdfService;
import com.ceramax.api.service.venta.PagoSimuladoService;
import com.ceramax.api.service.venta.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pedidos")
@Validated
public class PedidoController {

    private final PedidoService pedidoService;
    private final PagoSimuladoService pagoSimuladoService;
    private final ComprobantePdfService comprobantePdfService;

    public PedidoController(
        PedidoService pedidoService,
        PagoSimuladoService pagoSimuladoService,
        ComprobantePdfService comprobantePdfService
    ) {
        this.pedidoService = pedidoService;
        this.pagoSimuladoService = pagoSimuladoService;
        this.comprobantePdfService = comprobantePdfService;
    }

    @PostMapping("/web/checkout")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PedidoResponse> iniciarCheckoutWeb(
        @Valid @RequestBody CrearPedidoWebRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoSimuladoService.confirmar(request));
    }

    @PostMapping("/web/checkout/cotizacion")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<CheckoutCotizacionResponse> cotizarCheckoutWeb(
        @Valid @RequestBody CotizarCheckoutWebRequest request
    ) {
        return ResponseEntity.ok(pedidoService.cotizarCheckoutWeb(request));
    }

    @PostMapping("/presencial")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<VentaPresencialResponse> registrarVentaPresencial(
        @Valid @RequestBody VentaPresencialRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.registrarVentaPresencial(request));
    }

    @GetMapping("/mios")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<PedidoResponse>> listarMisPedidos() {
        return ResponseEntity.ok(pedidoService.listarMisPedidos());
    }

    @GetMapping("/mios/pagina")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PedidoPaginaResponse> listarMisPedidosPagina(
        @RequestParam(defaultValue = "0") @Min(0) int pagina,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano
    ) {
        return ResponseEntity.ok(pedidoService.listarPaginaVisible(PageRequest.of(pagina, tamano)));
    }

    @PostMapping("/{pedidoId}/cancelacion")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PedidoResponse> cancelarPedidoCliente(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(pedidoService.cancelarPedidoCliente(pedidoId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<List<PedidoResponse>> listarPedidos() {
        return ResponseEntity.ok(pedidoService.listarPedidosVisibles());
    }

    @GetMapping("/delivery/historial")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'DELIVERY')")
    public ResponseEntity<List<PedidoResponse>> listarHistorialDelivery() {
        return ResponseEntity.ok(pedidoService.listarHistorialDelivery());
    }

    @GetMapping("/{pedidoId}/historial-estados")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'DELIVERY')")
    public ResponseEntity<List<PedidoEstadoHistorialResponse>> listarHistorialEstados(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(pedidoService.listarHistorialEstados(pedidoId));
    }

    @GetMapping("/logistica")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<List<PedidoResponse>> listarBandejaLogistica() {
        return ResponseEntity.ok(pedidoService.listarBandejaLogistica());
    }

    @GetMapping("/deliveries")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<List<DeliveryOpcionResponse>> listarDeliveriesActivos() {
        return ResponseEntity.ok(pedidoService.listarDeliveriesActivos());
    }

    @GetMapping("/pagina")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<PedidoPaginaResponse> listarPedidosPagina(
        @RequestParam(defaultValue = "0") @Min(0) int pagina,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano
    ) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        return ResponseEntity.ok(pedidoService.listarPaginaVisible(pageable));
    }

    @GetMapping("/{pedidoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY', 'CLIENTE')")
    public ResponseEntity<PedidoResponse> obtener(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(pedidoService.obtener(pedidoId));
    }

    @GetMapping(value = "/{pedidoId}/comprobante/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'CLIENTE')")
    public ResponseEntity<byte[]> descargarComprobante(@PathVariable UUID pedidoId) {
        byte[] pdf = comprobantePdfService.generar(pedidoId);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"comprobante-" + pedidoId + ".pdf\""
            )
            .body(pdf);
    }

    @PatchMapping("/{pedidoId}/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'VENDEDOR', 'DELIVERY')")
    public ResponseEntity<PedidoResponse> cambiarEstado(
        @PathVariable UUID pedidoId,
        @Valid @RequestBody CambiarEstadoPedidoRequest request
    ) {
        return ResponseEntity.ok(pedidoService.cambiarEstado(pedidoId, request));
    }

    @GetMapping("/{pedidoId}/transiciones")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA', 'DELIVERY')")
    public ResponseEntity<List<String>> listarTransicionesDisponibles(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(pedidoService.listarTransicionesDisponibles(pedidoId));
    }

    @PutMapping("/{pedidoId}/delivery")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_LOGISTICA', 'LOGISTICA')")
    public ResponseEntity<PedidoResponse> asignarDelivery(
        @PathVariable UUID pedidoId,
        @Valid @RequestBody AsignarDeliveryRequest request
    ) {
        return ResponseEntity.ok(pedidoService.asignarDelivery(pedidoId, request));
    }
}
