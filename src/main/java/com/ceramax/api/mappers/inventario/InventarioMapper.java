package com.ceramax.api.mappers.inventario;

import com.ceramax.api.dto.response.AjusteInventarioResponse;
import com.ceramax.api.dto.response.InventarioResponse;
import com.ceramax.api.dto.response.MovimientoInventarioResponse;
import com.ceramax.api.dto.response.ReservaInventarioResponse;
import com.ceramax.api.dto.response.StockDisponibleResponse;
import com.ceramax.api.dto.response.TrasladoInventarioResponse;
import com.ceramax.api.repository.inventario.AjusteInventarioProjection;
import com.ceramax.api.repository.inventario.InventarioProjection;
import com.ceramax.api.repository.inventario.MovimientoInventarioProjection;
import com.ceramax.api.repository.inventario.ReservaInventarioProjection;
import com.ceramax.api.repository.inventario.TrasladoInventarioProjection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class InventarioMapper {

    public InventarioResponse toInventario(InventarioProjection row) {
        return new InventarioResponse(
            row.getIdSucursal(),
            row.getSucursalNombre(),
            row.getIdAlmacen(),
            row.getAlmacenCodigo(),
            row.getAlmacenNombre(),
            row.getIdProducto(),
            row.getProductoCodigo(),
            row.getProductoNombre(),
            row.getCategoriaNombre(),
            row.getStockFisico(),
            row.getStockReservado(),
            row.getStockDisponible(),
            row.getStockMinimo(),
            row.getStockBajo()
        );
    }

    public StockDisponibleResponse toStockDisponible(InventarioProjection row) {
        return new StockDisponibleResponse(
            row.getIdSucursal(),
            row.getSucursalNombre(),
            row.getIdAlmacen(),
            row.getAlmacenCodigo(),
            row.getAlmacenNombre(),
            row.getIdProducto(),
            row.getProductoCodigo(),
            row.getProductoNombre(),
            row.getCategoriaNombre(),
            row.getStockFisico(),
            row.getStockReservado(),
            row.getStockDisponible()
        );
    }

    public MovimientoInventarioResponse toMovimiento(MovimientoInventarioProjection row) {
        return new MovimientoInventarioResponse(
            row.getId(),
            row.getCodigo(),
            toOffsetDateTime(row.getCreatedAt()),
            row.getIdAlmacen(),
            row.getAlmacenNombre(),
            row.getIdProducto(),
            row.getProductoCodigo(),
            row.getProductoNombre(),
            row.getIdUsuario(),
            row.getUsuarioNombre(),
            row.getIdPedido(),
            row.getIdTraslado(),
            row.getTipo(),
            row.getMotivo(),
            row.getCantidad(),
            row.getStockResultante(),
            row.getObservacion()
        );
    }

    public TrasladoInventarioResponse toTraslado(TrasladoInventarioProjection row) {
        return new TrasladoInventarioResponse(
            row.getId(),
            row.getCodigo(),
            row.getTipoTraslado(),
            row.getEstado(),
            row.getIdAlmacenOrigen(),
            row.getAlmacenOrigenNombre(),
            row.getIdAlmacenDestino(),
            row.getAlmacenDestinoNombre(),
            row.getIdProducto(),
            row.getProductoCodigo(),
            row.getProductoNombre(),
            row.getCantidad(),
            row.getIdPedido(),
            row.getPedidoCodigo(),
            toOffsetDateTime(row.getFechaInicio()),
            row.getDiasTransito()
        );
    }

    public AjusteInventarioResponse toAjuste(AjusteInventarioProjection row) {
        return new AjusteInventarioResponse(
            row.getId(),
            row.getCodigo(),
            row.getIdUsuarioEjecutor(),
            row.getUsuarioEjecutor(),
            row.getIdAlmacen(),
            row.getAlmacenNombre(),
            row.getIdProducto(),
            row.getProductoCodigo(),
            row.getProductoNombre(),
            row.getStockAnterior(),
            row.getCantidadAjustada(),
            row.getStockNuevo(),
            row.getMotivo(),
            row.getObservacion(),
            toOffsetDateTime(row.getCreatedAt())
        );
    }

    public ReservaInventarioResponse toReserva(ReservaInventarioProjection row) {
        return new ReservaInventarioResponse(
            row.getIdReserva(),
            row.getIdPedido(),
            row.getPedidoCodigo(),
            row.getIdDetallePedido(),
            row.getIdAlmacen(),
            row.getAlmacenNombre(),
            row.getIdProducto(),
            row.getCantidad(),
            row.getEstado(),
            row.getTipoEntrega(),
            row.getClienteNombre(),
            toOffsetDateTime(row.getFechaPedido()),
            toOffsetDateTime(row.getFechaReserva())
        );
    }

    private OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
