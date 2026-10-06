package com.ceramax.api.repository.venta;

import com.ceramax.api.model.venta.Pago;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, UUID> {
    List<Pago> findByPedido_IdOrderByCreatedAtAsc(UUID pedidoId);

    @EntityGraph(attributePaths = "pedido")
    List<Pago> findByPedido_IdInOrderByPedido_IdAscCreatedAtAsc(List<UUID> pedidoIds);

    Optional<Pago> findByTransaccionId(String transaccionId);
}
