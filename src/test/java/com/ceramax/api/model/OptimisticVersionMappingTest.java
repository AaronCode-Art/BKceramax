package com.ceramax.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ceramax.api.model.catalogo.Producto;
import com.ceramax.api.model.venta.Pedido;
import jakarta.persistence.Column;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

class OptimisticVersionMappingTest {

    @Test
    void pedidoTieneVersionJpaMapeadaAColumnaVersion() throws NoSuchFieldException {
        var field = Pedido.class.getDeclaredField("version");

        assertNotNull(field.getAnnotation(Version.class));
        assertEquals("version", field.getAnnotation(Column.class).name());
    }

    @Test
    void productoTieneVersionJpaMapeadaAColumnaVersion() throws NoSuchFieldException {
        var field = Producto.class.getDeclaredField("version");

        assertNotNull(field.getAnnotation(Version.class));
        assertEquals("version", field.getAnnotation(Column.class).name());
    }
}
