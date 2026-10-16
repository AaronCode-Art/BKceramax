package com.ceramax.api.service.catalogo;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ceramax.api.exception.BusinessException;
import com.ceramax.api.exception.ExternalServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

    @CircuitBreaker(name = "cloudinary", fallbackMethod = "uploadFallback")
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

    @CircuitBreaker(name = "cloudinary", fallbackMethod = "chatUploadFallback")
    public ChatCloudinaryAsset uploadChatAttachment(MultipartFile file, UUID chatId) {
        String extension = validateChatAttachment(file);
        if (!isConfigured()) {
            throw new BusinessException("Cloudinary no está configurado completamente en el backend.");
        }

        boolean image = file.getContentType().startsWith("image/");
        String resourceType = image ? "image" : "raw";
        String folder = rootFolder.replaceAll("^/+|/+$", "")
            + "/chatinterno/" + chatId + (image ? "/images" : "/archivos");
        String publicId = folder + "/" + UUID.randomUUID() + (image ? "" : "." + extension);
        Map<String, Object> options = ObjectUtils.asMap(
            "resource_type", resourceType,
            "overwrite", false,
            "unique_filename", false,
            "public_id", publicId
        );
        options.put("asset_folder", folder);

        try {
            Map<?, ?> result = cloudinary().uploader().upload(file.getBytes(), options);
            Object secureUrl = result.get("secure_url");
            Object uploadedPublicId = result.get("public_id");
            if (!(secureUrl instanceof String url) || !(uploadedPublicId instanceof String resultPublicId)) {
                throw new BusinessException("Cloudinary devolvió una respuesta de archivo incompleta.");
            }
            return new ChatCloudinaryAsset(url, resultPublicId, resourceType);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            LOGGER.error("Cloudinary chat attachment upload failed for chat {}", chatId, exception);
            throw new BusinessException("No se pudo cargar el archivo al chat.");
        }
    }

    public ChatCloudinaryAsset chatUploadFallback(MultipartFile file, UUID chatId, Exception ex) {
        LOGGER.error("Cloudinary chat attachment upload unavailable for chat {}", chatId, ex);
        throw new ExternalServiceUnavailableException(
            "El servicio de archivos no está disponible. El adjunto no se cargó; intente más tarde.",
            ex
        );
    }

    @CircuitBreaker(name = "cloudinary", fallbackMethod = "chatDeleteFallback")
    public void deleteChatAttachment(String publicId, String resourceType) {
        if (!isConfigured()) {
            throw new BusinessException("Cloudinary no está configurado completamente en el backend.");
        }
        try {
            Map<?, ?> result = cloudinary().uploader().destroy(publicId, ObjectUtils.asMap(
                "resource_type", resourceType,
                "invalidate", true
            ));
            Object status = result.get("result");
            if (!"ok".equals(status) && !"not found".equals(status)) {
                throw new BusinessException("Cloudinary no confirmó la eliminación del archivo.");
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            LOGGER.error("Cloudinary chat attachment delete failed for public ID {}", publicId, exception);
            throw new BusinessException("No se pudo limpiar el archivo cargado al chat.");
        }
    }

    public void chatDeleteFallback(String publicId, String resourceType, Exception ex) {
        LOGGER.error("Cloudinary chat attachment delete unavailable for public ID {}", publicId, ex);
        throw new ExternalServiceUnavailableException(
            "El servicio de archivos no está disponible para limpiar el adjunto.",
            ex
        );
    }

    public CloudinaryAsset uploadFallback(
        MultipartFile file,
        String folder,
        String existingPublicId,
        Exception ex
    ) {
        LOGGER.error("Cloudinary upload unavailable; image was not uploaded", ex);
        throw new ExternalServiceUnavailableException(
            "El servicio de imágenes no está disponible. La imagen no se cargó; intente más tarde.",
            ex
        );
    }

    @CircuitBreaker(name = "cloudinary", fallbackMethod = "deleteFallback")
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

    public void deleteFallback(String publicId, Exception ex) {
        LOGGER.error("Cloudinary delete unavailable; image {} was not confirmed deleted", publicId, ex);
        throw new ExternalServiceUnavailableException(
            "El servicio de imágenes no está disponible. No se confirmó la eliminación; intente más tarde.",
            ex
        );
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

    public String reviewImageFolder() {
        String normalizedRoot = rootFolder.replaceAll("^/+|/+$", "");
        return normalizedRoot + "/resenas/imagenes";
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

    private String validateChatAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Selecciona un archivo para adjuntar.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("El archivo supera el límite de 10 MB.");
        }
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename();
        if (contentType == null || originalName == null || originalName.isBlank()) {
            throw new BusinessException("El archivo debe incluir nombre y tipo de contenido.");
        }
        String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        boolean validType = switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> extension.equals("jpg") || extension.equals("jpeg");
            case "image/png" -> extension.equals("png");
            case "image/webp" -> extension.equals("webp");
            case "image/gif" -> extension.equals("gif");
            case "image/avif" -> extension.equals("avif");
            case "application/pdf" -> extension.equals("pdf");
            case "text/plain" -> extension.equals("txt");
            case "text/csv" -> extension.equals("csv");
            case "application/msword" -> extension.equals("doc");
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                extension.equals("docx");
            case "application/vnd.ms-excel" -> extension.equals("xls");
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                extension.equals("xlsx");
            default -> false;
        };
        if (!validType) {
            throw new BusinessException("Tipo de archivo no permitido. Usa imágenes, PDF, DOCX, XLSX, TXT o CSV.");
        }
        return extension;
    }

    public record CloudinaryAsset(String url, String publicId) {}
    public record ChatCloudinaryAsset(String url, String publicId, String resourceType) {}
}
