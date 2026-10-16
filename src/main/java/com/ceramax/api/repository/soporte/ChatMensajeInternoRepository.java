package com.ceramax.api.repository.soporte;

import com.ceramax.api.model.soporte.ChatMensajeInterno;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMensajeInternoRepository extends JpaRepository<ChatMensajeInterno, UUID> {
    Page<ChatMensajeInterno> findByIdChatOrderByCreatedAtAscIdAsc(UUID idChat, Pageable pageable);
}
