package com.ceramax.api.observer.listener;

import static org.mockito.Mockito.verify;

import com.ceramax.api.observer.event.StockBajoEvent;
import com.ceramax.api.observer.event.TrasladoRecibidoEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class InventarioEventListenerTest {

    private final SimpMessagingTemplate messagingTemplate =
        org.mockito.Mockito.mock(SimpMessagingTemplate.class);
    private final InventarioEventListener listener = new InventarioEventListener(messagingTemplate);

    @Test
    void publicaAlertaDeStockBajoEnTopicoLogistico() {
        StockBajoEvent event = new StockBajoEvent(
            UUID.randomUUID(),
            "Almacén principal",
            UUID.randomUUID(),
            "PRO-0001",
            "Maceta",
            2,
            5,
            UUID.randomUUID()
        );

        listener.notificarStockBajo(event);

        verify(messagingTemplate).convertAndSend("/topic/logistica/alertas", event);
    }

    @Test
    void publicaRecepcionDeTrasladoEnTopicoLogistico() {
        TrasladoRecibidoEvent event = new TrasladoRecibidoEvent(
            UUID.randomUUID(),
            "TRA-000001",
            UUID.randomUUID(),
            "Almacén destino",
            UUID.randomUUID(),
            "PRO-0001",
            "Maceta",
            4,
            UUID.randomUUID()
        );

        listener.notificarTrasladoRecibido(event);

        verify(messagingTemplate).convertAndSend("/topic/logistica/traslados", event);
    }
}
