package com.ceramax.api.observer.listener;

import static org.mockito.Mockito.verify;

import com.ceramax.api.dto.response.MensajeChatResponse;
import com.ceramax.api.observer.event.MensajeChatEvent;
import com.ceramax.api.observer.event.PedidoEstadoCambiadoEvent;
import com.ceramax.api.service.auditoria.AuditoriaService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditoriaEventListenerTest {

    private final AuditoriaService auditoriaService = org.mockito.Mockito.mock(AuditoriaService.class);
    private final AuditoriaEventListener listener = new AuditoriaEventListener(auditoriaService);

    @Test
    void auditaCambioDePedidoConActorYEstadoAnterior() {
        UUID pedidoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        listener.auditarPedidoActualizado(new PedidoEstadoCambiadoEvent(
            pedidoId,
            "PED-000010",
            "EN_RUTA",
            "En ruta",
            "cliente@example.com",
            null,
            "LISTO_DESPACHO",
            usuarioId,
            "CAMBIAR_ESTADO"
        ));

        ArgumentCaptor<Object> anteriores = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> nuevos = ArgumentCaptor.forClass(Object.class);
        verify(auditoriaService).registrar(
            org.mockito.ArgumentMatchers.eq(usuarioId),
            org.mockito.ArgumentMatchers.eq("CAMBIAR_ESTADO"),
            org.mockito.ArgumentMatchers.eq("pedido"),
            org.mockito.ArgumentMatchers.eq(pedidoId),
            org.mockito.ArgumentMatchers.anyString(),
            anteriores.capture(),
            nuevos.capture()
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "LISTO_DESPACHO",
            ((java.util.Map<?, ?>) anteriores.getValue()).get("estado")
        );
        org.junit.jupiter.api.Assertions.assertEquals(
            "EN_RUTA",
            ((java.util.Map<?, ?>) nuevos.getValue()).get("estado")
        );
    }

    @Test
    void auditaMensajeSinAlmacenarSuContenidoPrivado() {
        UUID chatId = UUID.randomUUID();
        UUID mensajeId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        MensajeChatResponse mensaje = new MensajeChatResponse(
            mensajeId,
            chatId,
            clienteId,
            "CLIENTE",
            "contenido privado",
            false,
            OffsetDateTime.now()
        );

        listener.auditarMensajeChat(new MensajeChatEvent("soporte", chatId, mensaje));

        ArgumentCaptor<Object> nuevos = ArgumentCaptor.forClass(Object.class);
        verify(auditoriaService).registrar(
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq("ENVIAR_MENSAJE_SOPORTE"),
            org.mockito.ArgumentMatchers.eq("mensajesoporte"),
            org.mockito.ArgumentMatchers.eq(mensajeId),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.isNull(),
            nuevos.capture()
        );
        java.util.Map<?, ?> datos = (java.util.Map<?, ?>) nuevos.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(chatId, datos.get("chatId"));
        org.junit.jupiter.api.Assertions.assertFalse(datos.containsKey("contenido"));
    }
}
