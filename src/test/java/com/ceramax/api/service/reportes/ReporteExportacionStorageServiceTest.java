package com.ceramax.api.service.reportes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ceramax.api.model.reportes.ReporteExportacion.FormatoReporte;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReporteExportacionStorageServiceTest {

    @TempDir
    Path directorioTemporal;

    @Test
    void guardaYLeeElArchivoConUnNombreDerivadoDelUuid() throws Exception {
        ReporteExportacionStorageService storage =
            new ReporteExportacionStorageService(directorioTemporal.toString());
        UUID id = UUID.randomUUID();
        byte[] contenido = new byte[] {4, 5, 6};

        String nombre = storage.guardar(id, FormatoReporte.EXCEL, contenido);

        assertEquals(id + ".xlsx", nombre);
        assertArrayEquals(contenido, storage.leer(nombre));
        assertTrue(Files.exists(directorioTemporal.resolve(nombre)));
    }

    @Test
    void rechazaNombresQueEscapanDelDirectorioConfigurado() {
        ReporteExportacionStorageService storage =
            new ReporteExportacionStorageService(directorioTemporal.toString());

        assertThrows(IllegalArgumentException.class, () -> storage.leer("..\\secreto.txt"));
    }
}
