package com.ceramax.api.repository.inventario;

import com.ceramax.api.dto.response.SolicitudEntradaResponse;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InventarioOperacionRepository {

    private final JdbcTemplate jdbcTemplate;

    public InventarioOperacionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Integer consultarStockDisponible(UUID almacenId, UUID productoId) {
        return jdbcTemplate.queryForObject(
            "SELECT fn_stock_disponible_almacen(?::uuid, ?::uuid)",
            Integer.class,
            almacenId,
            productoId
        );
    }

    public boolean registrarEntrada(UUID almacenId, UUID productoId, int cantidad, UUID usuarioId, String observacion) {
        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(
                "SELECT registrar_entrada_proveedor(?::uuid, ?::uuid, ?::integer, ?::uuid, ?::varchar)",
                Boolean.class,
                almacenId,
                productoId,
                cantidad,
                usuarioId,
                observacion
            )
        );
    }

    public UUID registrarAjuste(
        UUID almacenId,
        UUID productoId,
        int nuevoStock,
        String motivo,
        String observacion,
        UUID usuarioId
    ) {
        return jdbcTemplate.queryForObject(
            "SELECT registrar_ajuste_inventario(?::uuid, ?::uuid, ?::integer, ?::varchar, ?::varchar, ?::uuid)",
            UUID.class,
            almacenId,
            productoId,
            nuevoStock,
            motivo,
            observacion,
            usuarioId
        );
    }

    public UUID iniciarTraslado(
        UUID almacenOrigenId,
        UUID almacenDestinoId,
        UUID productoId,
        int cantidad,
        UUID usuarioId,
        String tipo,
        UUID pedidoId,
        UUID detallePedidoId,
        String observacion
    ) {
        return jdbcTemplate.queryForObject(
            "SELECT iniciar_traslado(?::uuid, ?::uuid, ?::uuid, ?::integer, ?::uuid, ?::varchar, ?::uuid, ?::uuid, ?::varchar)",
            UUID.class,
            almacenOrigenId,
            almacenDestinoId,
            productoId,
            cantidad,
            usuarioId,
            tipo,
            pedidoId,
            detallePedidoId,
            observacion
        );
    }

    public boolean confirmarRecepcion(UUID trasladoId, UUID usuarioId, String observacion) {
        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(
                "SELECT confirmar_recepcion_traslado(?::uuid, ?::uuid, ?::varchar)",
                Boolean.class,
                trasladoId,
                usuarioId,
                observacion
            )
        );
    }

    public boolean cancelarTraslado(UUID trasladoId, UUID usuarioId, String motivo) {
        return Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(
                "SELECT cancelar_traslado(?::uuid, ?::uuid, ?::varchar)",
                Boolean.class,
                trasladoId,
                usuarioId,
                motivo
            )
        );
    }

    public List<TrasladoDatos> buscarTraslado(UUID trasladoId) {
        return jdbcTemplate.query(
            """
                SELECT id, codigo, id_almacen_origen, id_almacen_destino,
                       almacen_destino_nombre, id_producto, producto_codigo,
                       producto_nombre, cantidad
                FROM traslado_inventario
                WHERE id = ?
                """,
            (resultSet, rowNumber) -> new TrasladoDatos(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("codigo"),
                resultSet.getObject("id_almacen_origen", UUID.class),
                resultSet.getObject("id_almacen_destino", UUID.class),
                resultSet.getString("almacen_destino_nombre"),
                resultSet.getObject("id_producto", UUID.class),
                resultSet.getString("producto_codigo"),
                resultSet.getString("producto_nombre"),
                resultSet.getInt("cantidad")
            ),
            trasladoId
        );
    }

    public record ProductoIngreso(
        UUID id,
        String codigo,
        String nombre,
        BigDecimal precioFinal
    ) {}

    public ProductoIngreso obtenerProductoIngreso(UUID productoId) {
        return jdbcTemplate.queryForObject(
            "SELECT id, codigo, nombre, precio_final FROM productos WHERE id = ?",
            (rs, rowNum) -> new ProductoIngreso(
                rs.getObject("id", UUID.class),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getBigDecimal("precio_final")
            ),
            productoId
        );
    }

    public UUID crearSolicitudEntrada(
        UUID almacenId,
        UUID productoId,
        int cantidad,
        String observacion,
        UUID usuarioSolicitanteId
    ) {
        return jdbcTemplate.queryForObject(
            """
            INSERT INTO solicitud_movimiento_inventario (
                tipo, id_almacen, almacen_nombre, id_producto, producto_codigo,
                producto_nombre, cantidad, motivo, observacion, estado,
                id_usuario_solicitante, usuario_solicitante_nombre
            )
            SELECT 'ENTRADA'::varchar, a.id, a.nombre, p.id, p.codigo, p.nombre, ?, 'REPOSICION', ?, 'PENDIENTE', u.id,
                   u.nombres || ' ' || u.apellidos
            FROM almacen a
            JOIN productos p ON p.id = ?
            JOIN usuarios u ON u.id = ?
            WHERE a.id = ? AND a.activo AND p.activo AND u.activo
            RETURNING id
            """,
            UUID.class,
            cantidad,
            observacion,
            productoId,
            usuarioSolicitanteId,
            almacenId
        );
    }

    public List<SolicitudEntradaResponse> listarSolicitudesEntrada(UUID solicitanteId) {
        String sql = """
            SELECT id, codigo, tipo, estado, id_almacen, almacen_nombre, id_producto,
                   producto_codigo, producto_nombre, cantidad, stock_resultante_esperado,
                   motivo, observacion, usuario_solicitante_nombre, usuario_aprobador_nombre,
                   fecha_solicitud, fecha_respuesta, motivo_respuesta
            FROM solicitud_movimiento_inventario
            WHERE tipo = 'ENTRADA'
            """;
        Object[] params = solicitanteId == null ? new Object[0] : new Object[] { solicitanteId };
        if (solicitanteId != null) {
            sql += " AND id_usuario_solicitante = ?";
        }
        sql += " ORDER BY fecha_solicitud DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new SolicitudEntradaResponse(
            rs.getObject("id", UUID.class),
            rs.getString("codigo"),
            rs.getString("tipo"),
            rs.getString("estado"),
            rs.getObject("id_almacen", UUID.class),
            rs.getString("almacen_nombre"),
            rs.getObject("id_producto", UUID.class),
            rs.getString("producto_codigo"),
            rs.getString("producto_nombre"),
            (Integer) rs.getObject("cantidad"),
            (Integer) rs.getObject("stock_resultante_esperado"),
            rs.getString("motivo"),
            rs.getString("observacion"),
            rs.getString("usuario_solicitante_nombre"),
            rs.getString("usuario_aprobador_nombre"),
            rs.getObject("fecha_solicitud", java.time.OffsetDateTime.class),
            rs.getObject("fecha_respuesta", java.time.OffsetDateTime.class),
            rs.getString("motivo_respuesta")
        ), params);
    }

    public List<SolicitudEntradaResponse> listarSolicitudesEntradaPendientes() {
        return jdbcTemplate.query(
            """
            SELECT id, codigo, tipo, estado, id_almacen, almacen_nombre, id_producto,
                   producto_codigo, producto_nombre, cantidad, stock_resultante_esperado,
                   motivo, observacion, usuario_solicitante_nombre, usuario_aprobador_nombre,
                   fecha_solicitud, fecha_respuesta, motivo_respuesta
            FROM solicitud_movimiento_inventario
            WHERE tipo = 'ENTRADA' AND estado = 'PENDIENTE'
            ORDER BY fecha_solicitud DESC
            """,
            (rs, rowNum) -> new SolicitudEntradaResponse(
                rs.getObject("id", UUID.class),
                rs.getString("codigo"),
                rs.getString("tipo"),
                rs.getString("estado"),
                rs.getObject("id_almacen", UUID.class),
                rs.getString("almacen_nombre"),
                rs.getObject("id_producto", UUID.class),
                rs.getString("producto_codigo"),
                rs.getString("producto_nombre"),
                (Integer) rs.getObject("cantidad"),
                (Integer) rs.getObject("stock_resultante_esperado"),
                rs.getString("motivo"),
                rs.getString("observacion"),
                rs.getString("usuario_solicitante_nombre"),
                rs.getString("usuario_aprobador_nombre"),
                rs.getObject("fecha_solicitud", java.time.OffsetDateTime.class),
                rs.getObject("fecha_respuesta", java.time.OffsetDateTime.class),
                rs.getString("motivo_respuesta")
            )
        );
    }

    public boolean actualizarEstadoSolicitudEntrada(UUID id, String estado, UUID usuarioAprobadorId, String motivoRespuesta) {
        return jdbcTemplate.update(
            """
            UPDATE solicitud_movimiento_inventario
            SET estado = ?, id_usuario_aprobador = ?, usuario_aprobador_nombre = (
                    SELECT nombres || ' ' || apellidos FROM usuarios WHERE id = ?
                ), fecha_respuesta = now(), motivo_respuesta = ?
            WHERE id = ? AND estado = 'PENDIENTE'
            """,
            estado,
            usuarioAprobadorId,
            usuarioAprobadorId,
            motivoRespuesta,
            id
        ) == 1;
    }

    public SolicitudEntradaDatos obtenerSolicitudEntrada(UUID id) {
        return jdbcTemplate.queryForObject(
            """
            SELECT id, id_almacen, almacen_nombre, id_producto, producto_codigo, producto_nombre, cantidad, observacion
            FROM solicitud_movimiento_inventario
            WHERE id = ? AND tipo = 'ENTRADA'
            """,
            (rs, rowNum) -> new SolicitudEntradaDatos(
                rs.getObject("id", UUID.class),
                rs.getObject("id_almacen", UUID.class),
                rs.getString("almacen_nombre"),
                rs.getObject("id_producto", UUID.class),
                rs.getString("producto_codigo"),
                rs.getString("producto_nombre"),
                rs.getInt("cantidad"),
                rs.getString("observacion")
            ),
            id
        );
    }

    public record SolicitudEntradaDatos(
        UUID id,
        UUID almacenId,
        String almacenNombre,
        UUID productoId,
        String productoCodigo,
        String productoNombre,
        Integer cantidad,
        String observacion
    ) {}

    public record TrasladoDatos(
        UUID id,
        String codigo,
        UUID almacenOrigenId,
        UUID almacenDestinoId,
        String almacenDestinoNombre,
        UUID productoId,
        String productoCodigo,
        String productoNombre,
        Integer cantidad
    ) {}
}
