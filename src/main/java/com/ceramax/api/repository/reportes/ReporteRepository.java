package com.ceramax.api.repository.reportes;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
public class ReporteRepository {

    private static final String SQL_INVENTARIO_ACTUAL = """
        SELECT id_almacen,
               almacen_codigo,
               almacen_nombre,
               id_producto,
               producto_codigo,
               producto_nombre,
               categoria_nombre,
               stock_fisico,
               stock_reservado,
               stock_disponible,
               stock_minimo,
               stock_bajo
        FROM vw_inventario
        ORDER BY almacen_nombre, producto_nombre
        """;
    private static final int FETCH_SIZE_INVENTARIO = 500;

    private final JdbcTemplate jdbcTemplate;

    public ReporteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResumenVentas obtenerResumenVentas(OffsetDateTime desde, OffsetDateTime hastaExclusivo) {
        return jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)::bigint AS cantidad_ventas,
                       COUNT(*) FILTER (WHERE p.canal = 'WEB')::bigint AS ventas_web,
                       COUNT(*) FILTER (WHERE p.canal = 'PRESENCIAL')::bigint AS ventas_presenciales,
                       COALESCE(SUM(p.total), 0)::numeric AS ingresos_totales
                FROM pedido p
                JOIN estadoenvio e ON e.id = p.id_estado
                JOIN pago pg ON pg.id_pedido = p.id AND pg.estado = 'APROBADO'
                WHERE p.created_at >= ?
                  AND p.created_at < ?
                  AND pg.proveedor IS DISTINCT FROM 'SIMULADO'
                  AND e.codigo <> 'CANCELADO'
                """,
            (resultSet, rowNumber) -> new ResumenVentas(
                resultSet.getLong("cantidad_ventas"),
                resultSet.getLong("ventas_web"),
                resultSet.getLong("ventas_presenciales"),
                resultSet.getBigDecimal("ingresos_totales")
            ),
            desde,
            hastaExclusivo
        );
    }

    public List<ProductoMasVendido> listarProductosMasVendidos(
        OffsetDateTime desde,
        OffsetDateTime hastaExclusivo
    ) {
        return jdbcTemplate.query(
            """
                SELECT d.id_producto,
                       d.producto_codigo,
                       d.producto_nombre,
                       SUM(d.cantidad)::bigint AS unidades_vendidas,
                       SUM(d.subtotal)::numeric AS importe_vendido
                FROM detallepedido d
                JOIN pedido p ON p.id = d.id_pedido
                JOIN estadoenvio e ON e.id = p.id_estado
                JOIN pago pg ON pg.id_pedido = p.id AND pg.estado = 'APROBADO'
                WHERE p.created_at >= ?
                  AND p.created_at < ?
                  AND pg.proveedor IS DISTINCT FROM 'SIMULADO'
                  AND e.codigo <> 'CANCELADO'
                GROUP BY d.id_producto, d.producto_codigo, d.producto_nombre
                ORDER BY unidades_vendidas DESC, d.producto_codigo
                LIMIT 10
                """,
            (resultSet, rowNumber) -> new ProductoMasVendido(
                resultSet.getObject("id_producto", UUID.class),
                resultSet.getString("producto_codigo"),
                resultSet.getString("producto_nombre"),
                resultSet.getLong("unidades_vendidas"),
                resultSet.getBigDecimal("importe_vendido")
            ),
            desde,
            hastaExclusivo
        );
    }

    public List<InventarioActual> listarInventarioActual() {
        return jdbcTemplate.query(
            SQL_INVENTARIO_ACTUAL,
            (resultSet, rowNumber) -> mapearInventario(resultSet)
        );
    }

    public void recorrerInventarioActual(Consumer<InventarioActual> consumidor) {
        jdbcTemplate.query(
            connection -> {
                PreparedStatement statement = connection.prepareStatement(SQL_INVENTARIO_ACTUAL);
                statement.setFetchSize(FETCH_SIZE_INVENTARIO);
                return statement;
            },
            (RowCallbackHandler) resultSet -> consumidor.accept(mapearInventario(resultSet))
        );
    }

    private InventarioActual mapearInventario(ResultSet resultSet) throws SQLException {
        return new InventarioActual(
            resultSet.getObject("id_almacen", UUID.class),
            resultSet.getString("almacen_codigo"),
            resultSet.getString("almacen_nombre"),
            resultSet.getObject("id_producto", UUID.class),
            resultSet.getString("producto_codigo"),
            resultSet.getString("producto_nombre"),
            resultSet.getString("categoria_nombre"),
            resultSet.getInt("stock_fisico"),
            resultSet.getInt("stock_reservado"),
            resultSet.getInt("stock_disponible"),
            resultSet.getInt("stock_minimo"),
            resultSet.getBoolean("stock_bajo")
        );
    }

    public record ResumenVentas(
        Long cantidadVentas,
        Long ventasWeb,
        Long ventasPresenciales,
        BigDecimal ingresosTotales
    ) {}

    public record ProductoMasVendido(
        UUID productoId,
        String productoCodigo,
        String productoNombre,
        Long unidadesVendidas,
        BigDecimal importeVendido
    ) {}

    public record InventarioActual(
        UUID almacenId,
        String almacenCodigo,
        String almacenNombre,
        UUID productoId,
        String productoCodigo,
        String productoNombre,
        String categoriaNombre,
        Integer stockFisico,
        Integer stockReservado,
        Integer stockDisponible,
        Integer stockMinimo,
        Boolean stockBajo
    ) {}
}
