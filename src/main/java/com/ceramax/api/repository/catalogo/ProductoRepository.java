package com.ceramax.api.repository.catalogo;

import com.ceramax.api.model.catalogo.Producto;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, UUID id);

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByIdIn(Collection<UUID> ids);
}
