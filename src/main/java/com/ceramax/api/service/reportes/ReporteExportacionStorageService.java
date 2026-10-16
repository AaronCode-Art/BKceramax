package com.ceramax.api.service.reportes;

import com.ceramax.api.model.reportes.ReporteExportacion.FormatoReporte;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.DirectoryStream;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ReporteExportacionStorageService {

    private final Path directorio;

    public ReporteExportacionStorageService(@Value("${reports.storage-dir:./var/reportes}") String directorio) {
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    }

    public String guardar(UUID id, FormatoReporte formato, byte[] contenido) throws IOException {
        Files.createDirectories(directorio);
        String nombre = nombreArchivo(id, formato);
        Path destino = resolver(nombre);
        Path temporal = resolver(id + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.write(temporal, contenido, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try {
                Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporal, destino);
            }
            return nombre;
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    public byte[] leer(String nombreArchivo) throws IOException {
        return Files.readAllBytes(resolver(nombreArchivo));
    }

    public void eliminar(String nombreArchivo) throws IOException {
        Files.deleteIfExists(resolver(nombreArchivo));
    }

    public void eliminar(UUID id, FormatoReporte formato) throws IOException {
        eliminar(nombreArchivo(id, formato));
    }

    public void limpiarTemporales() throws IOException {
        if (!Files.isDirectory(directorio)) {
            return;
        }
        try (DirectoryStream<Path> temporales = Files.newDirectoryStream(directorio, "*.tmp")) {
            for (Path temporal : temporales) {
                Files.deleteIfExists(temporal);
            }
        }
    }

    private String nombreArchivo(UUID id, FormatoReporte formato) {
        return id + (formato == FormatoReporte.PDF ? ".pdf" : ".xlsx");
    }

    private Path resolver(String nombreArchivo) {
        Path archivo = directorio.resolve(nombreArchivo).normalize();
        if (!archivo.startsWith(directorio)) {
            throw new IllegalArgumentException("Ruta de archivo de reporte inválida");
        }
        return archivo;
    }
}
