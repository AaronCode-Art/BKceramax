package com.ceramax.api.repository.soporte;

import com.ceramax.api.model.soporte.ChatInterno;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatInternoRepository extends JpaRepository<ChatInterno, UUID> {
    @Query("""
        select chat
        from ChatInterno chat
        where exists (
            select miembro.id
            from ChatInternoMiembro miembro
            where miembro.idChat = chat.id and miembro.idUsuario = :usuarioId
        )
        order by chat.updatedAt desc, chat.id desc
        """)
    Page<ChatInterno> findVisibleToMember(@Param("usuarioId") UUID usuarioId, Pageable pageable);
}
