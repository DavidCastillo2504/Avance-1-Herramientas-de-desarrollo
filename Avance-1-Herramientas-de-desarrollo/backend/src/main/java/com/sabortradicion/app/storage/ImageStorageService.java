package com.sabortradicion.app.storage;

import com.sabortradicion.app.dto.ImageUploadResponse;
import com.sabortradicion.app.exception.BusinessRuleException;
import com.sabortradicion.app.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * Sube imágenes del menú al bucket S3 configurado en S3_BUCKET, bajo el prefijo "menu/".
 * Las credenciales se obtienen de la cadena por defecto del SDK (en EC2: el IAM Role de la instancia),
 * nunca del código. Las imágenes se sirven directamente desde S3.
 */
@Service
public class ImageStorageService {
    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final String PREFIX = "menu/";

    private final String bucket;
    private final String region;
    private final String publicBaseUrl;
    private volatile S3Client client;

    @Autowired
    public ImageStorageService(@Value("${app.s3.bucket:}") String bucket,
                               @Value("${app.s3.region:us-east-1}") String region,
                               @Value("${app.s3.public-base-url:}") String publicBaseUrl) {
        this.bucket = bucket == null ? "" : bucket.trim();
        this.region = region == null || region.isBlank() ? "us-east-1" : region.trim();
        String base = publicBaseUrl == null || publicBaseUrl.isBlank()
                ? "https://" + this.bucket + ".s3." + this.region + ".amazonaws.com"
                : publicBaseUrl.trim();
        this.publicBaseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    /** Constructor para pruebas con un cliente simulado. */
    ImageStorageService(String bucket, String region, String publicBaseUrl, S3Client client) {
        this(bucket, region, publicBaseUrl);
        this.client = client;
    }

    public boolean isEnabled() {
        return !bucket.isEmpty();
    }

    public ImageUploadResponse upload(MultipartFile file) {
        if (!isEnabled()) {
            throw new BusinessRuleException("La carga de imágenes no está configurada en el servidor (S3_BUCKET). "
                    + "Sube la imagen al bucket manualmente y pega su URL.");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Selecciona una imagen");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessRuleException("La imagen no puede superar 5 MB");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessRuleException("No se pudo leer la imagen enviada");
        }
        ImageType type = ImageType.detect(bytes);
        if (type == null) {
            throw new BusinessRuleException("Formato no permitido. Usa imágenes JPG, PNG o WEBP");
        }

        String key = PREFIX + UUID.randomUUID() + "." + type.extension;
        try {
            s3().putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(type.contentType)
                            .cacheControl("public, max-age=31536000, immutable")
                            .build(),
                    RequestBody.fromBytes(bytes));
        } catch (SdkException exception) {
            throw new ExternalServiceException("No se pudo guardar la imagen en S3. Revisa el bucket y el IAM Role.",
                    exception);
        }
        return new ImageUploadResponse(publicBaseUrl + "/" + key, key);
    }

    private S3Client s3() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    client = S3Client.builder().region(Region.of(region)).build();
                }
            }
        }
        return client;
    }

    /** El tipo se decide por el contenido real del archivo, no por la extensión ni por lo que declara el cliente. */
    enum ImageType {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp");

        final String contentType;
        final String extension;

        ImageType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        static ImageType detect(byte[] b) {
            if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
                return JPEG;
            }
            if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                    && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
                return PNG;
            }
            if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
                return WEBP;
            }
            return null;
        }
    }
}
