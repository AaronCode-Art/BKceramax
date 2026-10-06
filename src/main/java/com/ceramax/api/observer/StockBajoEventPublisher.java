package com.ceramax.api.observer;

import com.ceramax.api.observer.event.StockBajoEvent;
import com.ceramax.api.repository.inventario.InventarioConsultaRepository;
import com.ceramax.api.repository.inventario.InventarioEstadoProjection;
import com.ceramax.api.repository.inventario.MovimientoInventarioProjection;
import com.ceramax.api.repository.inventario.ReservaInventarioProjection;
import com.ceramax.api.repository.inventario.StockBajoProjection;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class StockBajoEventPublisher {

    private final InventarioConsultaRepository inventarioConsultaRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StockBajoEventPublisher(
        InventarioConsultaRepository inventarioConsultaRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.inventarioConsultaRepository = inventarioConsultaRepository;
        this.eventPublisher = eventPublisher;
    }

    public StockSnapshot capturarProductos(Collection<UUID> productoIds) {
        List<UUID> ids = productoIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return new StockSnapshot(Map.of());
        }
        Map<InventarioUbicacion, Boolean> estado = new HashMap<>();
        for (InventarioEstadoProjection inventario : inventarioConsultaRepository.obtenerEstadoPorProductos(ids)) {
            estado.put(
                new InventarioUbicacion(inventario.getIdAlmacen(), inventario.getIdProducto()),
                Boolean.TRUE.equals(inventario.getStockBajo())
            );
        }
        return new StockSnapshot(Map.copyOf(estado));
    }

    public StockSnapshot capturarPedido(UUID pedidoId) {
        return capturarProductos(inventarioConsultaRepository.listarProductosPedido(pedidoId));
    }

    public StockSnapshot capturarUbicacion(UUID almacenId, UUID productoId) {
        boolean stockBajo = inventarioConsultaRepository.obtenerStockBajo(almacenId, productoId).isPresent();
        return new StockSnapshot(Map.of(new InventarioUbicacion(almacenId, productoId), stockBajo));
    }

    public void notificarCrucesVentaPresencial(StockSnapshot estadoAnterior, UUID pedidoId, UUID actorId) {
        List<MovimientoInventarioProjection> movimientos =
            inventarioConsultaRepository.listarSalidasVentaPresencial(pedidoId);
        notificarCruces(
            estadoAnterior,
            movimientos.stream()
                .map(movimiento -> new InventarioUbicacion(
                    movimiento.getIdAlmacen(),
                    movimiento.getIdProducto()
                ))
                .toList(),
            actorId
        );
    }

    public void notificarCrucesReservaPedido(StockSnapshot estadoAnterior, UUID pedidoId, UUID actorId) {
        List<ReservaInventarioProjection> reservas =
            inventarioConsultaRepository.listarReservasActivasPedido(pedidoId);
        notificarCruces(
            estadoAnterior,
            reservas.stream()
                .map(reserva -> new InventarioUbicacion(reserva.getIdAlmacen(), reserva.getIdProducto()))
                .toList(),
            actorId
        );
    }

    public void notificarCruces(
        StockSnapshot estadoAnterior,
        Collection<InventarioUbicacion> ubicaciones,
        UUID actorId
    ) {
        for (InventarioUbicacion ubicacion : new LinkedHashSet<>(ubicaciones)) {
            if (Boolean.TRUE.equals(estadoAnterior.stockBajoPorUbicacion().get(ubicacion))) {
                continue;
            }
            inventarioConsultaRepository.obtenerStockBajo(ubicacion.almacenId(), ubicacion.productoId())
                .ifPresent(stock -> eventPublisher.publishEvent(toEvent(stock, actorId)));
        }
    }

    private StockBajoEvent toEvent(StockBajoProjection stock, UUID actorId) {
        return new StockBajoEvent(
            stock.getIdAlmacen(),
            stock.getAlmacenNombre(),
            stock.getIdProducto(),
            stock.getProductoCodigo(),
            stock.getProductoNombre(),
            stock.getStockDisponible(),
            stock.getStockMinimo(),
            actorId
        );
    }

    public record InventarioUbicacion(UUID almacenId, UUID productoId) {}

    public record StockSnapshot(Map<InventarioUbicacion, Boolean> stockBajoPorUbicacion) {}
}
