package com.ceramax.api.repository.venta;

import com.ceramax.api.model.venta.DetallePedido;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, UUID> {
    List<DetallePedido> findByPedido_IdOrderByIdAsc(UUID pedidoId);

    @EntityGraph(attributePaths = {"pedido", "producto"})
    List<DetallePedido> findByPedido_IdInOrderByPedido_IdAscIdAsc(List<UUID> pedidoIds);
}
