package com.ceramax.api.observer.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ceramax.api.exception.ExternalServiceUnavailableException;
import com.ceramax.api.observer.event.PedidoCreadoEvent;
import com.ceramax.api.service.soporte.EmailService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class PedidoEventListenerTest {

    @Test
    void falloDelCorreoAsincronoQuedaRegistradoSinFingirEntrega() {
        EmailService emailService = mock(EmailService.class);
        PedidoEventListener listener = new PedidoEventListener(emailService, mock(SimpMessagingTemplate.class));
        PedidoCreadoEvent event = new PedidoCreadoEvent(
            UUID.randomUUID(),
            "PED-1",
            "WEB",
            "PENDIENTE",
            "Pendiente",
            "cliente@example.com",
            UUID.randomUUID()
        );
        when(emailService.enviarHtml(
            "cliente@example.com",
            "Pedido PED-1 registrado",
            "<p>Tu pedido <strong>PED-1</strong> fue registrado. Estado: Pendiente.</p>"
        )).thenReturn(CompletableFuture.failedFuture(new ExternalServiceUnavailableException(
            "No se pudo enviar el correo; el destinatario no fue notificado.",
            new IllegalStateException("SMTP no disponible")
        )));

        assertDoesNotThrow(() -> listener.notificarCreacion(event));
    }
}
