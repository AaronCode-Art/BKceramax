package com.ceramax.api.observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceramax.api.observer.StockBajoEventPublisher.InventarioUbicacion;
import com.ceramax.api.observer.StockBajoEventPublisher.StockSnapshot;
import com.ceramax.api.observer.event.StockBajoEvent;
import com.ceramax.api.repository.inventario.InventarioConsultaRepository;
import com.ceramax.api.repository.inventario.InventarioEstadoProjection;
import com.ceramax.api.repository.inventario.MovimientoInventarioProjection;
import com.ceramax.api.repository.inventario.ReservaInventarioProjection;
import com.ceramax.api.repository.inventario.StockBajoProjection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class StockBajoEventPublisherTest {

    private final InventarioConsultaRepository inventarioRepository = mock(InventarioConsultaRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final StockBajoEventPublisher publisher =
        new StockBajoEventPublisher(inventarioRepository, eventPublisher);

    @Test
    void capturaElEstadoDeStockDeLosProductosAntesDeLaOperacion() {
        UUID almacenId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        InventarioEstadoProjection inventario = mock(InventarioEstadoProjection.class);
        when(inventario.getIdAlmacen()).thenReturn(almacenId);
        when(inventario.getIdProducto()).thenReturn(productoId);
        when(inventario.getStockBajo()).thenReturn(false);
        when(inventarioRepository.obtenerEstadoPorProductos(List.of(productoId))).thenReturn(List.of(inventario));

        StockSnapshot snapshot = publisher.capturarProductos(List.of(productoId));

        assertEquals(
            Boolean.FALSE,
            snapshot.stockBajoPorUbicacion().get(new InventarioUbicacion(almacenId, productoId))
        );
    }

    @Test
    void publicaUnaAlertaSoloAlCruzarElUmbralEnLaUbicacionAfectada() {
        UUID almacenId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        InventarioUbicacion ubicacion = new InventarioUbicacion(almacenId, productoId);
        StockSnapshot snapshot = new StockSnapshot(Map.of(ubicacion, false));
        StockBajoProjection stock = stockBajo(almacenId, productoId);
        when(inventarioRepository.obtenerStockBajo(almacenId, productoId)).thenReturn(Optional.of(stock));

        publisher.notificarCruces(snapshot, List.of(ubicacion, ubicacion), actorId);

        verify(eventPublisher).publishEvent(new StockBajoEvent(
            almacenId,
            "Almacén principal",
            productoId,
            "PRO-0001",
            "Maceta",
            2,
            5,
            actorId
        ));
    }

    @Test
    void noRepiteAlertaSiElStockYaEstabaBajoAntesDeLaOperacion() {
        UUID almacenId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        InventarioUbicacion ubicacion = new InventarioUbicacion(almacenId, productoId);
        StockSnapshot snapshot = new StockSnapshot(Map.of(ubicacion, true));

        publisher.notificarCruces(snapshot, List.of(ubicacion), UUID.randomUUID());

        verify(inventarioRepository, never()).obtenerStockBajo(almacenId, productoId);
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resuelveLaUbicacionDeLaVentaPresencialAntesDePublicarLaAlerta() {
        UUID almacenId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        UUID pedidoId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        MovimientoInventarioProjection movimiento = mock(MovimientoInventarioProjection.class);
        when(movimiento.getIdAlmacen()).thenReturn(almacenId);
        when(movimiento.getIdProducto()).thenReturn(productoId);
        when(inventarioRepository.listarSalidasVentaPresencial(pedidoId)).thenReturn(List.of(movimiento));
        StockBajoProjection stock = stockBajo(almacenId, productoId);
        when(inventarioRepository.obtenerStockBajo(almacenId, productoId)).thenReturn(Optional.of(stock));

        publisher.notificarCrucesVentaPresencial(
            new StockSnapshot(Map.of(new InventarioUbicacion(almacenId, productoId), false)),
            pedidoId,
            actorId
        );

        verify(inventarioRepository).listarSalidasVentaPresencial(pedidoId);
        verify(eventPublisher).publishEvent(new StockBajoEvent(
            almacenId,
            "Almacén principal",
            productoId,
            "PRO-0001",
            "Maceta",
            2,
            5,
            actorId
        ));
    }

    @Test
    void resuelveLaUbicacionDeLaReservaWebAntesDePublicarLaAlerta() {
        UUID almacenId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        UUID pedidoId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ReservaInventarioProjection reserva = mock(ReservaInventarioProjection.class);
        when(reserva.getIdAlmacen()).thenReturn(almacenId);
        when(reserva.getIdProducto()).thenReturn(productoId);
        when(inventarioRepository.listarReservasActivasPedido(pedidoId)).thenReturn(List.of(reserva));
        StockBajoProjection stock = stockBajo(almacenId, productoId);
        when(inventarioRepository.obtenerStockBajo(almacenId, productoId)).thenReturn(Optional.of(stock));

        publisher.notificarCrucesReservaPedido(
            new StockSnapshot(Map.of(new InventarioUbicacion(almacenId, productoId), false)),
            pedidoId,
            actorId
        );

        verify(inventarioRepository).listarReservasActivasPedido(pedidoId);
        verify(eventPublisher).publishEvent(new StockBajoEvent(
            almacenId,
            "Almacén principal",
            productoId,
            "PRO-0001",
            "Maceta",
            2,
            5,
            actorId
        ));
    }

    private StockBajoProjection stockBajo(UUID almacenId, UUID productoId) {
        StockBajoProjection stock = mock(StockBajoProjection.class);
        when(stock.getIdAlmacen()).thenReturn(almacenId);
        when(stock.getAlmacenNombre()).thenReturn("Almacén principal");
        when(stock.getIdProducto()).thenReturn(productoId);
        when(stock.getProductoCodigo()).thenReturn("PRO-0001");
        when(stock.getProductoNombre()).thenReturn("Maceta");
        when(stock.getStockDisponible()).thenReturn(2);
        when(stock.getStockMinimo()).thenReturn(5);
        return stock;
    }
}
