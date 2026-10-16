package com.ceramax.api.service.soporte;

import com.ceramax.api.exception.ExternalServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailDeliveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDeliveryService.class);
    private final JavaMailSender javaMailSender;

    public EmailDeliveryService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @CircuitBreaker(name = "email", fallbackMethod = "enviarHtmlFallback")
    public void enviarHtml(String destinatario, String asunto, String contenidoHtml) {
        try {
            MimeMessage mensaje = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(contenidoHtml, true);
            javaMailSender.send(mensaje);
        } catch (MessagingException exception) {
            throw new IllegalStateException("No se pudo enviar el correo", exception);
        }
    }

    public void enviarHtmlFallback(String destinatario, String asunto, String contenidoHtml, Exception exception) {
        LOGGER.error("Email delivery failed for {}. Subject: {}", destinatario, asunto, exception);
        throw new ExternalServiceUnavailableException(
            "No se pudo enviar el correo; el destinatario no fue notificado.",
            exception
        );
    }
}
