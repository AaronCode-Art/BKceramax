package com.ceramax.api.repository.catalogo;

import com.ceramax.api.model.catalogo.GaleriaImagen;
import com.ceramax.api.repository.catalogo.ProductoImagenProjection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface GaleriaImagenRepository extends JpaRepository<GaleriaImagen, UUID> {
    List<GaleriaImagen> findByProducto_IdOrderByOrdenAsc(UUID productoId);

    @Query(
        value = """
            SELECT id_producto AS "productoId", url
            FROM galeriaimagen
            WHERE id_producto IN (:productoIds)
            ORDER BY id_producto, es_principal DESC, orden ASC, created_at ASC
            """,
        nativeQuery = true
    )
    List<ProductoImagenProjection> listarImagenPrincipalPorProductos(
        @Param("productoIds") List<UUID> productoIds
    );
}
