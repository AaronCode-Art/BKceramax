package com.ceramax.api.repository.inventario;

import com.ceramax.api.model.inventario.Almacen;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface InventarioConsultaRepository extends Repository<Almacen, UUID> {

    @Query(
        value = """
            SELECT id_almacen AS "idAlmacen",
                   almacen_nombre AS "almacenNombre",
                   id_producto AS "idProducto",
                   producto_codigo AS "productoCodigo",
                   producto_nombre AS "productoNombre",
                   stock_disponible AS "stockDisponible",
                   stock_minimo AS "stockMinimo"
            FROM vw_inventario
            WHERE id_almacen = :almacenId
              AND id_producto = :productoId
              AND stock_bajo = TRUE
            """,
        nativeQuery = true
    )
    Optional<StockBajoProjection> obtenerStockBajo(
        @Param("almacenId") UUID almacenId,
        @Param("productoId") UUID productoId
    );

    @Query(
        value = """
            SELECT id_almacen AS "idAlmacen",
                   id_producto AS "idProducto",
                   stock_bajo AS "stockBajo"
            FROM vw_inventario
            WHERE id_producto IN (:productoIds)
            """,
        nativeQuery = true
    )
    List<InventarioEstadoProjection> obtenerEstadoPorProductos(
        @Param("productoIds") Collection<UUID> productoIds
    );

    @Query(
        value = """
            SELECT DISTINCT id_producto
            FROM detallepedido
            WHERE id_pedido = :pedidoId
              AND id_producto IS NOT NULL
            """,
        nativeQuery = true
    )
    List<UUID> listarProductosPedido(@Param("pedidoId") UUID pedidoId);

    @Query(
        value = """
            SELECT id, codigo, created_at AS "createdAt", id_almacen AS "idAlmacen",
                   almacen_nombre AS "almacenNombre", id_producto AS "idProducto",
                   producto_codigo AS "productoCodigo", producto_nombre AS "productoNombre",
                   id_usuario AS "idUsuario", usuario_nombre AS "usuarioNombre",
                   id_pedido AS "idPedido", id_traslado AS "idTraslado", tipo, motivo,
                   cantidad, stock_resultante AS "stockResultante", observacion
            FROM vw_movimientos_inventario
            WHERE id_pedido = :pedidoId
              AND tipo = 'SALIDA'
              AND motivo = 'VENTA_PRESENCIAL'
            """,
        nativeQuery = true
    )
    List<MovimientoInventarioProjection> listarSalidasVentaPresencial(
        @Param("pedidoId") UUID pedidoId
    );

    @Query(
        value = """
            SELECT id_reserva AS "idReserva", id_pedido AS "idPedido",
                   pedido_codigo AS "pedidoCodigo", id_detalle_pedido AS "idDetallePedido",
                   id_almacen AS "idAlmacen", almacen_nombre AS "almacenNombre",
                   id_producto AS "idProducto", cantidad, estado, tipo_entrega AS "tipoEntrega",
                   cliente_nombre AS "clienteNombre", fecha_pedido AS "fechaPedido",
                   fecha_reserva AS "fechaReserva"
            FROM vw_reservas_activas
            WHERE id_pedido = :pedidoId
            """,
        nativeQuery = true
    )
    List<ReservaInventarioProjection> listarReservasActivasPedido(
        @Param("pedidoId") UUID pedidoId
    );

    @Query(
        value = """
            SELECT id_sucursal AS "idSucursal", sucursal_nombre AS "sucursalNombre",
                   id_almacen AS "idAlmacen", almacen_codigo AS "almacenCodigo",
                   almacen_nombre AS "almacenNombre", id_producto AS "idProducto",
                   producto_codigo AS "productoCodigo", producto_nombre AS "productoNombre",
                   categoria_nombre AS "categoriaNombre", stock_fisico AS "stockFisico",
                   stock_reservado AS "stockReservado", stock_disponible AS "stockDisponible",
                   stock_minimo AS "stockMinimo", stock_bajo AS "stockBajo"
            FROM vw_inventario
            ORDER BY almacen_nombre, producto_nombre, id_almacen, id_producto
            """,
        countQuery = "SELECT COUNT(*) FROM vw_inventario",
        nativeQuery = true
    )
    Page<InventarioProjection> listarInventario(Pageable pageable);

    @Query(
        value = """
            SELECT id_sucursal AS "idSucursal", sucursal_nombre AS "sucursalNombre",
                   id_almacen AS "idAlmacen", almacen_codigo AS "almacenCodigo",
                   almacen_nombre AS "almacenNombre", id_producto AS "idProducto",
                   producto_codigo AS "productoCodigo", producto_nombre AS "productoNombre",
                   categoria_nombre AS "categoriaNombre", stock_fisico AS "stockFisico",
                   stock_reservado AS "stockReservado", stock_disponible AS "stockDisponible"
            FROM vw_stock_disponible
            ORDER BY almacen_nombre, producto_nombre, id_almacen, id_producto
            """,
        countQuery = "SELECT COUNT(*) FROM vw_stock_disponible",
        nativeQuery = true
    )
    Page<InventarioProjection> listarStockDisponible(Pageable pageable);

    @Query(
        value = """
            SELECT id, codigo, created_at AS "createdAt", id_almacen AS "idAlmacen",
                   almacen_nombre AS "almacenNombre", id_producto AS "idProducto",
                   producto_codigo AS "productoCodigo", producto_nombre AS "productoNombre",
                   id_usuario AS "idUsuario", usuario_nombre AS "usuarioNombre",
                   id_pedido AS "idPedido", id_traslado AS "idTraslado", tipo, motivo,
                   cantidad, stock_resultante AS "stockResultante", observacion
            FROM vw_movimientos_inventario
            ORDER BY created_at DESC, id DESC
            """,
        countQuery = "SELECT COUNT(*) FROM vw_movimientos_inventario",
        nativeQuery = true
    )
    Page<MovimientoInventarioProjection> listarMovimientos(Pageable pageable);

    @Query(
        value = """
            SELECT id, codigo, tipo_traslado AS "tipoTraslado", estado,
                   id_almacen_origen AS "idAlmacenOrigen",
                   almacen_origen_nombre AS "almacenOrigenNombre",
                   id_almacen_destino AS "idAlmacenDestino",
                   almacen_destino_nombre AS "almacenDestinoNombre",
                   id_producto AS "idProducto", producto_codigo AS "productoCodigo",
                   producto_nombre AS "productoNombre", cantidad, id_pedido AS "idPedido",
                   pedido_codigo AS "pedidoCodigo", fecha_inicio AS "fechaInicio",
                   dias_transito AS "diasTransito"
            FROM vw_traslados_en_transito
            ORDER BY fecha_inicio, id
            """,
        countQuery = "SELECT COUNT(*) FROM vw_traslados_en_transito",
        nativeQuery = true
    )
    Page<TrasladoInventarioProjection> listarTrasladosEnTransito(Pageable pageable);

    @Query(
        value = """
            SELECT id_reserva AS "idReserva", id_pedido AS "idPedido",
                   pedido_codigo AS "pedidoCodigo", id_detalle_pedido AS "idDetallePedido",
                   id_almacen AS "idAlmacen", almacen_nombre AS "almacenNombre",
                   id_producto AS "idProducto", cantidad, estado, tipo_entrega AS "tipoEntrega",
                   cliente_nombre AS "clienteNombre", fecha_pedido AS "fechaPedido",
                   fecha_reserva AS "fechaReserva"
            FROM vw_reservas_activas
            ORDER BY fecha_reserva, id_reserva
            """,
        countQuery = "SELECT COUNT(*) FROM vw_reservas_activas",
        nativeQuery = true
    )
    Page<ReservaInventarioProjection> listarReservasActivas(Pageable pageable);

    @Query(
        value = """
            SELECT id, codigo, id_usuario_ejecutor AS "idUsuarioEjecutor",
                   usuario_ejecutor AS "usuarioEjecutor", id_almacen AS "idAlmacen",
                   almacen_nombre AS "almacenNombre", id_producto AS "idProducto",
                   producto_codigo AS "productoCodigo", producto_nombre AS "productoNombre",
                   stock_anterior AS "stockAnterior", cantidad_ajustada AS "cantidadAjustada",
                   stock_nuevo AS "stockNuevo", motivo, observacion, created_at AS "createdAt"
            FROM ajuste_inventario_log
            ORDER BY created_at DESC, id DESC
            """,
        countQuery = "SELECT COUNT(*) FROM ajuste_inventario_log",
        nativeQuery = true
    )
    Page<AjusteInventarioProjection> listarAjustes(Pageable pageable);
}
