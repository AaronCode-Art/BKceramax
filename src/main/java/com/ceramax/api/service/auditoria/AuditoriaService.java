package com.ceramax.api.service.auditoria;

import com.ceramax.api.repository.auditoria.AuditoriaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(
        UUID usuarioId,
        String accion,
        String tabla,
        UUID registroId,
        String descripcion,
        Object datosAnteriores,
        Object datosNuevos
    ) {
        auditoriaRepository.registrar(
            usuarioId,
            accion,
            tabla,
            registroId,
            descripcion,
            serializar(datosAnteriores),
            serializar(datosNuevos)
        );
    }

    private String serializar(Object datos) {
        if (datos == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudieron serializar los datos de auditoría", exception);
        }
    }
}
