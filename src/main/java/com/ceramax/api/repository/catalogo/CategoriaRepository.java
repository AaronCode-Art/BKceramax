package com.ceramax.api.repository.catalogo;

import com.ceramax.api.model.catalogo.Categoria;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {
    boolean existsByCodigo(String codigo);
    boolean existsByNombre(String nombre);
    boolean existsByCodigoAndIdNot(String codigo, UUID id);
    boolean existsByNombreAndIdNot(String nombre, UUID id);
}
