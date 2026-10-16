package com.ceramax.api.service.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ExternalServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CloudinaryStorageServiceTest {

    @Test
    void normalizaNombresDeCategoriaYProductoParaLaRutaCloudinary() {
        assertEquals("ceramica-piso", CloudinaryStorageService.slug("Cerámica Piso"));
        CloudinaryStorageService service = new CloudinaryStorageService("", "", "", "ceramax");
        assertEquals("ceramax/ceramica-piso/porcelanato-blanco", service.productFolder("Cerámica Piso", "Porcelanato Blanco"));
        assertEquals("ceramax/resenas/imagenes", service.reviewImageFolder());
    }

    @Test
    void informaClaramenteCuandoFaltanCredencialesDeCloudinary() {
        CloudinaryStorageService service = new CloudinaryStorageService("", "", "", "ceramax");
        MockMultipartFile image = new MockMultipartFile(
            "archivo",
            "producto.png",
            "image/png",
            new byte[] {1, 2, 3}
        );

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.upload(image, "ceramax/categoria/producto", null)
        );

        assertEquals("Cloudinary no está configurado completamente en el backend.", exception.getMessage());
    }

    @Test
    void elFallbackDeCargaInformaQueLaImagenNoSeGuardo() {
        CloudinaryStorageService service = new CloudinaryStorageService("", "", "", "ceramax");

        ExternalServiceUnavailableException exception = assertThrows(
            ExternalServiceUnavailableException.class,
            () -> service.uploadFallback(
                new MockMultipartFile("archivo", "producto.png", "image/png", new byte[] {1}),
                "ceramax/producto",
                null,
                new IllegalStateException("Cloudinary no disponible")
            )
        );

        assertEquals(
            "El servicio de imágenes no está disponible. La imagen no se cargó; intente más tarde.",
            exception.getMessage()
        );
    }

    @Test
    void elFallbackDeEliminacionNoPermiteReportarExito() {
        CloudinaryStorageService service = new CloudinaryStorageService("", "", "", "ceramax");

        ExternalServiceUnavailableException exception = assertThrows(
            ExternalServiceUnavailableException.class,
            () -> service.deleteFallback("ceramax/producto/imagen", new IllegalStateException("timeout"))
        );

        assertEquals(
            "El servicio de imágenes no está disponible. No se confirmó la eliminación; intente más tarde.",
            exception.getMessage()
        );
    }

    @Test
    void rechazaTiposDeArchivoNoPermitidosEnAdjuntosDeChat() {
        CloudinaryStorageService service = new CloudinaryStorageService("", "", "", "ceramax");
        MockMultipartFile executable = new MockMultipartFile(
            "archivo",
            "programa.exe",
            "application/octet-stream",
            new byte[] {1}
        );

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> service.uploadChatAttachment(executable, java.util.UUID.randomUUID())
        );

        assertEquals(
            "Tipo de archivo no permitido. Usa imágenes, PDF, DOCX, XLSX, TXT o CSV.",
            exception.getMessage()
        );
    }
}
