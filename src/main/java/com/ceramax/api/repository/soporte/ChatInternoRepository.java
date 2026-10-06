package com.ceramax.api.repository.soporte;

import com.ceramax.api.model.soporte.ChatInterno;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatInternoRepository extends JpaRepository<ChatInterno, UUID> {

    @Query("""
        select c from ChatInterno c
        where c.idUsuarioCreador = :usuarioId or c.idUsuarioDestino = :usuarioId
        order by c.updatedAt desc
        """)
    List<ChatInterno> findVisibleForUser(@Param("usuarioId") UUID usuarioId);
}
