package com.ceramax.api.service.catalogo;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ceramax.api.exception.BusinessException;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudinaryStorageService.class);
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private final String apiKey;
    private final String apiSecret;
    private final String cloudName;
    private final String rootFolder;

    public CloudinaryStorageService(
        @Value("${app.cloudinary.api-key:}") String apiKey,
        @Value("${app.cloudinary.api-secret:}") String apiSecret,
        @Value("${app.cloudinary.cloud-name:}") String cloudName,
        @Value("${app.cloudinary.folder:ceramax}") String rootFolder
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiSecret = apiSecret == null ? "" : apiSecret.trim();
        this.cloudName = cloudName == null ? "" : cloudName.trim();
        this.rootFolder = rootFolder == null || rootFolder.isBlank() ? "ceramax" : rootFolder.trim();
    }

    public CloudinaryAsset upload(MultipartFile file, String folder, String existingPublicId) {
        validateFile(file);
        if (!isConfigured()) {
            throw new BusinessException("Cloudinary no está configurado completamente en el backend.");
        }

        boolean newAsset = existingPublicId == null || existingPublicId.isBlank();
        String publicId = newAsset ? folder + "/" + UUID.randomUUID() : existingPublicId;
        Map<String, Object> options = ObjectUtils.asMap(
            "resource_type", "image",
            "overwrite", !newAsset,
            "unique_filename", false,
            "public_id", publicId
        );
        if (newAsset) {
            options.put("asset_folder", folder);
        }

        try {
            Map<?, ?> result = cloudinary().uploader().upload(file.getBytes(), options);
            Object secureUrl = result.get("secure_url");
            Object uploadedPublicId = result.get("public_id");
            if (!(secureUrl instanceof String url) || !(uploadedPublicId instanceof String resultPublicId)) {
                throw new BusinessException("Cloudinary devolvió una respuesta de imagen incompleta.");
            }
            return new CloudinaryAsset(url, resultPublicId);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            LOGGER.error("Cloudinary image upload failed", exception);
            throw new BusinessException("No se pudo cargar la imagen a Cloudinary.");
        }
    }

    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        if (!isConfigured()) {
            throw new BusinessException("Cloudinary no está configurado completamente en el backend.");
        }
        try {
            Map<?, ?> result = cloudinary().uploader().destroy(publicId, ObjectUtils.asMap(
                "resource_type", "image",
                "invalidate", true
            ));
            Object status = result.get("result");
            if (!"ok".equals(status) && !"not found".equals(status)) {
                throw new BusinessException("Cloudinary no confirmó la eliminación de la imagen.");
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            LOGGER.error("Cloudinary image deletion failed for public ID {}", publicId, exception);
            throw new BusinessException("No se pudo eliminar la imagen de Cloudinary.");
        }
    }

    public static String slug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "sin-nombre" : normalized;
    }

    public String productFolder(String categoryName, String productName) {
        String normalizedRoot = rootFolder.replaceAll("^/+|/+$", "");
        return normalizedRoot + "/"
            + slug(categoryName)
            + "/"
            + slug(productName);
    }

    private boolean isConfigured() {
        return !apiKey.isBlank() && !apiSecret.isBlank() && !cloudName.isBlank();
    }

    private Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        ));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Selecciona una imagen para cargar.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("La imagen supera el límite de 10 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.matches("image/(jpeg|png|webp|gif|avif)")) {
            throw new BusinessException("El archivo debe ser una imagen JPEG, PNG, WEBP, GIF o AVIF.");
        }
    }

    public record CloudinaryAsset(String url, String publicId) {}
}
