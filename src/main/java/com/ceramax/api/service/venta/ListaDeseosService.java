package com.ceramax.api.service.venta;

import com.ceramax.api.exception.ResourceNotFoundException;
import com.ceramax.api.model.acceso.Cliente;
import com.ceramax.api.repository.acceso.ClienteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ListaDeseosService {

    private final JdbcTemplate jdbcTemplate;
    private final ClienteRepository clienteRepository;

    public ListaDeseosService(JdbcTemplate jdbcTemplate, ClienteRepository clienteRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.clienteRepository = clienteRepository;
    }

    public List<UUID> obtener() {
        UUID listaId = obtenerOCrearListaId();
        return jdbcTemplate.query("""
            SELECT d.id_producto
            FROM detallelistadeseos d
            JOIN vw_catalogo vc ON vc.id_producto = d.id_producto
            WHERE d.id_lista_deseos = ?
            ORDER BY d.created_at DESC
            """, (row, rowNumber) -> row.getObject(1, UUID.class), listaId);
    }

    public List<UUID> agregar(UUID productoId) {
        UUID listaId = obtenerOCrearListaId();
        validarProducto(productoId);
        jdbcTemplate.update("""
            INSERT INTO detallelistadeseos (id_lista_deseos, id_producto)
            VALUES (?, ?)
            ON CONFLICT (id_lista_deseos, id_producto) DO NOTHING
            """, listaId, productoId);
        return obtener();
    }

    public List<UUID> eliminar(UUID productoId) {
        jdbcTemplate.update("""
            DELETE FROM detallelistadeseos
            WHERE id_lista_deseos = ? AND id_producto = ?
            """, obtenerOCrearListaId(), productoId);
        return obtener();
    }

    private UUID obtenerOCrearListaId() {
        UUID clienteId = clienteAutenticado().getId();
        return jdbcTemplate.queryForObject("""
            INSERT INTO listadeseos (id_cliente)
            VALUES (?)
            ON CONFLICT (id_cliente) DO UPDATE SET updated_at = now()
            RETURNING id
            """, UUID.class, clienteId);
    }

    private void validarProducto(UUID productoId) {
        try {
            jdbcTemplate.queryForObject(
                "SELECT id_producto FROM vw_catalogo WHERE id_producto = ?",
                UUID.class,
                productoId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw new ResourceNotFoundException("Producto activo no encontrado");
        }
    }

    private Cliente clienteAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return clienteRepository.findByEmail(email)
            .filter(Cliente::getActivo)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente activo autenticado no encontrado"));
    }
}
