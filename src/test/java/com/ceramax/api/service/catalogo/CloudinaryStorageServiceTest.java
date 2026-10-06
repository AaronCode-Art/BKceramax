package com.ceramax.api.service.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ceramax.api.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CloudinaryStorageServiceTest {

    @Test
    void normalizaNombresDeCategoriaYProductoParaLaRutaCloudinary() {
        assertEquals("ceramica-piso", CloudinaryStorageService.slug("Cerámica Piso"));
        assertEquals(
            "ceramax/ceramica-piso/porcelanato-blanco",
            new CloudinaryStorageService("", "", "", "ceramax")
                .productFolder("Cerámica Piso", "Porcelanato Blanco")
        );
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
}
