package com.ceramax.api.repository.soporte;

import com.ceramax.api.model.soporte.MensajeSoporte;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensajeSoporteRepository extends JpaRepository<MensajeSoporte, UUID> {
    List<MensajeSoporte> findByIdChatOrderByCreatedAtAsc(UUID idChat);
}
