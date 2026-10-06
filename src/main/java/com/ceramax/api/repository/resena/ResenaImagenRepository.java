package com.ceramax.api.repository.resena;

import com.ceramax.api.model.resena.ResenaImagen;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResenaImagenRepository extends JpaRepository<ResenaImagen, UUID> {

    List<ResenaImagen> findByIdResenaInOrderByIdResenaAscOrdenAsc(List<UUID> resenaIds);

    List<ResenaImagen> findByIdResenaOrderByOrdenAsc(UUID resenaId);

    void deleteByIdResena(UUID resenaId);
}
