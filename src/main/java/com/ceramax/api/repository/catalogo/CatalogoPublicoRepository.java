package com.ceramax.api.repository.catalogo;

import com.ceramax.api.model.catalogo.Producto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CatalogoPublicoRepository extends Repository<Producto, UUID> {

    @Query(
        value = """
            SELECT
                id_producto AS "idProducto",
                producto_codigo AS "productoCodigo",
                producto_nombre AS "productoNombre",
                categoria_nombre AS "categoriaNombre",
                descripcion AS "descripcion",
                precio AS "precio",
                precio_final AS "precioFinal",
                descuento_porcentaje AS "descuentoPorcentaje",
                stock_disponible AS "stockDisponible"
            FROM vw_catalogo
            ORDER BY producto_nombre, id_producto
            """,
        countQuery = "SELECT COUNT(*) FROM vw_catalogo",
        nativeQuery = true
    )
    Page<CatalogoPublicoProjection> listar(Pageable pageable);

    @Query(
        value = """
            SELECT
                id_producto AS "idProducto",
                producto_codigo AS "productoCodigo",
                producto_nombre AS "productoNombre",
                categoria_nombre AS "categoriaNombre",
                descripcion AS "descripcion",
                precio AS "precio",
                precio_final AS "precioFinal",
                descuento_porcentaje AS "descuentoPorcentaje",
                stock_disponible AS "stockDisponible"
            FROM vw_catalogo
            WHERE id_producto = :id
            """,
        nativeQuery = true
    )
    Optional<CatalogoPublicoProjection> buscarPorId(@Param("id") UUID id);
}
