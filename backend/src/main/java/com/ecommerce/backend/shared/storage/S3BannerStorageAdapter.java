package com.ecommerce.backend.shared.storage;

import com.ecommerce.backend.config.S3Properties;
import com.ecommerce.backend.exception.StorageException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class S3BannerStorageAdapter implements BannerStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3BannerStorageAdapter.class);

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @Override
    public void subir(String storageKey, InputStream contenido, long contentLength, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(storageKey)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(contenido, contentLength));
            log.info("Archivo subido exitosamente a S3. Bucket: {}, Key: {}", s3Properties.getBucket(), storageKey);
        } catch (Exception e) {
            log.error("Fallo al subir archivo a S3. Key: {}", storageKey, e);
            throw new StorageException("Error al interactuar con el servicio de almacenamiento de banners: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(String storageKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(storageKey)
                    .build();

            s3Client.deleteObject(request);
            log.info("Objeto eliminado exitosamente de S3. Bucket: {}, Key: {}", s3Properties.getBucket(), storageKey);
        } catch (Exception e) {
            log.warn("Fallo al eliminar objeto huérfano en S3. Bucket: {}, Key: {}. Requiere limpieza pendiente.",
                    s3Properties.getBucket(), storageKey, e);
        }
    }
}
