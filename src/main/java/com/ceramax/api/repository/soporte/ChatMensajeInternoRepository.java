package com.ceramax.api.repository.soporte;

import com.ceramax.api.model.soporte.ChatMensajeInterno;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMensajeInternoRepository extends JpaRepository<ChatMensajeInterno, UUID> {
    List<ChatMensajeInterno> findByIdChatOrderByCreatedAtAsc(UUID idChat);
}
