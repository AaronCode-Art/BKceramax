package com.ceramax.api.service.venta;

import com.ceramax.api.dto.request.ActualizarCarritoItemRequest;
import com.ceramax.api.dto.request.CarritoItemRequest;
import com.ceramax.api.dto.response.CarritoItemResponse;
import com.ceramax.api.dto.response.StoreProductResponse;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.repository.acceso.ClienteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CarritoService {

    private static final RowMapper<CarritoItemResponse> ITEM_ROW_MAPPER = (row, rowNumber) ->
        new CarritoItemResponse(
            new StoreProductResponse(
                row.getObject("producto_id", UUID.class),
                row.getString("producto_codigo"),
                row.getString("producto_nombre"),
                row.getString("categoria_nombre"),
                row.getString("descripcion"),
                row.getBigDecimal("precio"),
                row.getBigDecimal("precio_final"),
                row.getBigDecimal("descuento_porcentaje"),
                row.getInt("stock_disponible")
            ),
            row.getInt("cantidad")
        );

    private final JdbcTemplate jdbcTemplate;
    private final ClienteRepository clienteRepository;

    public CarritoService(JdbcTemplate jdbcTemplate, ClienteRepository clienteRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.clienteRepository = clienteRepository;
    }

    public List<CarritoItemResponse> obtener() {
        UUID clienteId = clienteAutenticado().getId();
        List<UUID> carritoIds = jdbcTemplate.query(
            "SELECT id FROM carrito WHERE id_cliente = ?",
            (row, rowNumber) -> row.getObject("id", UUID.class),
            clienteId
        );
        if (carritoIds.isEmpty()) {
            return List.of();
        }
        return obtenerDetalles(carritoIds.getFirst());
    }

    private List<CarritoItemResponse> obtenerDetalles(UUID carritoId) {
        return jdbcTemplate.query("""
            SELECT p.id AS producto_id,
                   p.codigo AS producto_codigo,
                   p.nombre AS producto_nombre,
                   c.nombre AS categoria_nombre,
                   p.descripcion,
                   p.precio,
                   p.precio_final,
                   p.descuento_porcentaje,
                   vc.stock_disponible,
                   cd.cantidad
            FROM carritodetalle cd
            JOIN productos p ON p.id = cd.id_producto
            JOIN categoria c ON c.id = p.id_categoria
            JOIN vw_catalogo vc ON vc.id_producto = p.id
            WHERE cd.id_carrito = ?
            ORDER BY cd.created_at
            """, ITEM_ROW_MAPPER, carritoId);
    }

    public List<CarritoItemResponse> agregar(CarritoItemRequest request) {
        UUID clienteId = clienteAutenticado().getId();
        UUID carritoId = obtenerOCrearCarritoId();
        jdbcTemplate.queryForObject(
            "SELECT id FROM carrito WHERE id = ? FOR UPDATE",
            UUID.class,
            carritoId
        );
        int stock = stockDisponible(request.productoId());
        int existente = cantidadActual(clienteId, request.productoId());
        if (existente + request.cantidad() > stock) {
            throw new BusinessException("La cantidad solicitada supera el stock disponible");
        }
        jdbcTemplate.update("""
            INSERT INTO carritodetalle (id_carrito, id_producto, cantidad)
            VALUES (?, ?, ?)
            ON CONFLICT (id_carrito, id_producto)
            DO UPDATE SET cantidad = carritodetalle.cantidad + EXCLUDED.cantidad,
                          updated_at = now()
            """, carritoId, request.productoId(), request.cantidad());
        return obtener();
    }

    public List<CarritoItemResponse> actualizar(UUID productoId, ActualizarCarritoItemRequest request) {
        UUID carritoId = obtenerOCrearCarritoId();
        int stock = stockDisponible(productoId);
        if (request.cantidad() > stock) {
            throw new BusinessException("La cantidad solicitada supera el stock disponible");
        }
        int updated = jdbcTemplate.update("""
            UPDATE carritodetalle
            SET cantidad = ?, updated_at = now()
            WHERE id_carrito = ? AND id_producto = ?
            """, request.cantidad(), carritoId, productoId);
        if (updated == 0) {
            throw new ResourceNotFoundException("El producto no está en el carrito");
        }
        return obtener();
    }

    public List<CarritoItemResponse> eliminar(UUID productoId) {
        jdbcTemplate.update("""
            DELETE FROM carritodetalle
            WHERE id_carrito = ? AND id_producto = ?
            """, obtenerOCrearCarritoId(), productoId);
        return obtener();
    }

    public void vaciar() {
        UUID clienteId = clienteAutenticado().getId();
        jdbcTemplate.update("""
            DELETE FROM carritodetalle cd
            USING carrito ca
            WHERE cd.id_carrito = ca.id AND ca.id_cliente = ?
            """, clienteId);
        jdbcTemplate.update("""
            UPDATE carrito SET estado = 'ACTIVO', updated_at = now()
            WHERE id_cliente = ?
            """, clienteId);
    }

    private UUID obtenerOCrearCarritoId() {
        UUID clienteId = clienteAutenticado().getId();
        return jdbcTemplate.queryForObject("""
            INSERT INTO carrito (id_cliente, estado)
            VALUES (?, 'ACTIVO')
            ON CONFLICT (id_cliente)
            DO UPDATE SET estado = 'ACTIVO', updated_at = now()
            RETURNING id
            """, UUID.class, clienteId);
    }

    private int stockDisponible(UUID productoId) {
        try {
            Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock_disponible FROM vw_catalogo WHERE id_producto = ?",
                Integer.class,
                productoId
            );
            return stock == null ? 0 : stock;
        } catch (EmptyResultDataAccessException exception) {
            throw new ResourceNotFoundException("Producto activo no encontrado");
        }
    }

    private int cantidadActual(UUID clienteId, UUID productoId) {
        List<Integer> cantidad = jdbcTemplate.query("""
            SELECT cd.cantidad
            FROM carritodetalle cd
            JOIN carrito ca ON ca.id = cd.id_carrito
            WHERE ca.id_cliente = ? AND cd.id_producto = ?
            """, (row, rowNumber) -> row.getInt(1), clienteId, productoId);
        return cantidad.isEmpty() ? 0 : cantidad.getFirst();
    }

    private Cliente clienteAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return clienteRepository.findByEmail(email)
            .filter(Cliente::getActivo)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente activo autenticado no encontrado"));
    }
}
