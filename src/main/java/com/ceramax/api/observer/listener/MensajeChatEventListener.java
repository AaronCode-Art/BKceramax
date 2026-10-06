package com.ceramax.api.observer.listener;

import com.ceramax.api.observer.event.MensajeChatEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MensajeChatEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public MensajeChatEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicarMensajePersistido(MensajeChatEvent event) {
        String destino = "soporte".equals(event.tipoChat())
            ? "/topic/chat/" + event.chatId()
            : "/topic/chat-interno/" + event.chatId();
        messagingTemplate.convertAndSend(
            destino,
            event.mensaje()
        );
    }
}
