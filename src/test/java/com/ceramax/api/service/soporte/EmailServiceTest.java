package com.ceramax.api.service.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import com.ceramax.api.exception.ExternalServiceUnavailableException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTest {

    @Test
    void elFallbackDeCorreoInformaQueElDestinatarioNoFueNotificado() {
        EmailDeliveryService service = new EmailDeliveryService(mock(JavaMailSender.class));

        ExternalServiceUnavailableException exception = assertThrows(
            ExternalServiceUnavailableException.class,
            () -> service.enviarHtmlFallback(
                "cliente@example.com",
                "Pedido creado",
                "<p>Pedido</p>",
                new IllegalStateException("SMTP no disponible")
            )
        );

        assertEquals("No se pudo enviar el correo; el destinatario no fue notificado.", exception.getMessage());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @Test
    void errorDeEnvioSePropagaComoFallo() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = mock(MimeMessage.class);
        org.mockito.Mockito.when(mailSender.createMimeMessage()).thenReturn(message);
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("SMTP no disponible"))
            .when(mailSender).send(message);
        EmailDeliveryService service = new EmailDeliveryService(mailSender);

        assertThrows(
            org.springframework.mail.MailSendException.class,
            () -> service.enviarHtml("cliente@example.com", "Pedido creado", "<p>Pedido</p>")
        );
    }
}
