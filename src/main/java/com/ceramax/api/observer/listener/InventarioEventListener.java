package com.ceramax.api.observer.listener;

import com.ceramax.api.observer.event.StockBajoEvent;
import com.ceramax.api.observer.event.TrasladoRecibidoEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InventarioEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public InventarioEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarStockBajo(StockBajoEvent event) {
        messagingTemplate.convertAndSend("/topic/logistica/alertas", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarTrasladoRecibido(TrasladoRecibidoEvent event) {
        messagingTemplate.convertAndSend("/topic/logistica/traslados", event);
    }
}
